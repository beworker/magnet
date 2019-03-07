package magnet.processor.instances

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.WildcardTypeName
import magnet.Classifier
import magnet.Instance
import magnet.Scope
import magnet.Scoping
import magnet.SelectorFilter
import magnet.processor.MagnetProcessorEnv
import magnet.processor.common.CompilationException
import magnet.processor.common.ValidationException
import magnet.processor.common.isOfAnnotationType
import magnet.processor.instances.disposer.DisposerAnnotationValidator
import magnet.processor.instances.disposer.DisposerAttributeParser
import magnet.processor.instances.factory.FactoryAttributeParser
import magnet.processor.instances.kotlin.MethodMetadata
import magnet.processor.instances.selector.SelectorAttributeParser
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import javax.lang.model.util.SimpleAnnotationValueVisitor6

const val FACTORY_SUFFIX = "MagnetFactory"
private const val CLASS_NULLABLE = ".Nullable"
private const val ATTR_TYPE = "type"
private const val ATTR_TYPES = "types"
private const val ATTR_SCOPING = "scoping"
private const val ATTR_CLASSIFIER = "classifier"
private const val ATTR_DISABLED = "disabled"

interface AttributeParser<R> {
    val attrName: String
    fun parse(value: AnnotationValue, element: Element): R
}

interface AnnotationValidator {
    fun validate(annotation: Annotation, element: Element)
}

internal abstract class AnnotationParser<in E : Element>(
    private val env: MagnetProcessorEnv,
    private val verifyInheritance: Boolean
) {

    protected val selectorAttributeParser = SelectorAttributeParser()

    private val typesAttrExtractor = TypesAttrExtractor(env.elements)
    private val factoryAttrParser = FactoryAttributeParser(env.annotation)
    private val disposerAttrParser = DisposerAttributeParser(env.annotation)
    private val validators = listOf<AnnotationValidator>(DisposerAnnotationValidator())

    protected fun parseMethodParameter(
        element: Element,
        variable: VariableElement,
        methodMetadata: MethodMetadata
    ): MethodParameter {

        val variableType = variable.asType()
        if (variableType.kind == TypeKind.TYPEVAR) {
            throw ValidationException(
                element = element,
                message = "Constructor parameter '${variable.simpleName}' is specified using a generic" +
                    " type which is not supported by Magnet. Use a non-parameterized class or interface" +
                    " type instead. To inject current scope instance, use 'Scope' parameter type."
            )
        }

        val paramSpec = ParameterSpec.get(variable)
        val paramName = paramSpec.name

        val isScopeParam = variableType.toString() == Scope::class.java.name
        if (isScopeParam) {
            return MethodParameter(
                name = PARAM_SCOPE_NAME,
                type = ClassName.get(Scope::class.java),
                typeErased = false,
                laziness = Laziness.None,
                classifier = Classifier.NONE,
                method = GetterMethod.GET_SCOPE
            )
        }

        var paramTypeName = paramSpec.type
        var getterMethod: GetterMethod? = null
        var laziness = Laziness.None

        var paramTypeErased = false
        paramTypeName = if (paramTypeName is ParameterizedTypeName) {

            when (paramTypeName.rawType.reflectionName()) {

                List::class.java.typeName -> {
                    getterMethod = GetterMethod.GET_MANY

                    paramTypeName
                        .extractGenericTypeName(element)
                        .let { (genericTypeName, typeErased) ->
                            paramTypeErased = typeErased
                            genericTypeName
                        }
                }

                Lazy::class.java.typeName -> {

                    // fixme handle cases, when methodMeta not available for java classes
                    // fixme handle Lazy<List<T>>

                    val typeMeta = methodMetadata.isParameterNullable(paramName, 1)
                    laziness = if (typeMeta.nullable) Laziness.Nullable
                    else Laziness.NotNullable

                    paramTypeName
                        .extractGenericTypeName(element)
                        .let { (genericTypeName, typeErased) ->
                            paramTypeErased = typeErased
                            genericTypeName
                        }
                }

                else -> {
                    if (!paramTypeName.typeArguments.isEmpty()) {
                        paramTypeErased = true
                    }
                    paramTypeName.rawType
                }
            }

        } else {
            ClassName.get(variableType)
        }

        paramTypeName = resolveWildcardParameterType(paramTypeName, element)

        var hasNullableAnnotation = false
        var classifier: String = Classifier.NONE

        variable.annotationMirrors.forEach { annotationMirror ->
            if (annotationMirror.isOfAnnotationType<Classifier>()) {
                val declaredClassifier: String? = annotationMirror.elementValues.values.firstOrNull()?.value.toString()
                declaredClassifier?.let {
                    classifier = it.removeSurrounding("\"", "\"")
                }

            } else {
                val annotationType = annotationMirror.annotationType.toString()
                if (annotationType.endsWith(CLASS_NULLABLE)) {
                    hasNullableAnnotation = true
                }
            }
        }

        if (getterMethod == null) {
            getterMethod = if (hasNullableAnnotation) GetterMethod.GET_OPTIONAL else GetterMethod.GET_SINGLE
        }

        return MethodParameter(
            name = paramName,
            type = paramTypeName,
            typeErased = paramTypeErased,
            laziness = laziness,
            classifier = classifier,
            method = getterMethod
        )
    }

    private fun ParameterizedTypeName.extractGenericTypeName(element: Element): Pair<TypeName, Boolean> {
        var paramTypeName = typeArguments[0]
        var paramTypeErased = false
        paramTypeName = resolveWildcardParameterType(paramTypeName, element)
        if (paramTypeName is ParameterizedTypeName) {
            if (!paramTypeName.typeArguments.isEmpty()) {
                paramTypeErased = true
                paramTypeName = paramTypeName.rawType
            }
        }
        return Pair(paramTypeName, paramTypeErased)
    }

    protected fun parseAnnotation(element: Element): Annotation {

        var interfaceTypeElement: TypeElement? = null
        var interfaceTypeElements: List<TypeElement>? = null
        var scoping = Scoping.TOPMOST.name
        var classifier = Classifier.NONE
        var selector = SelectorFilter.DEFAULT_SELECTOR
        var factory: TypeName? = null
        var disposer: String? = null
        var disabled = false

        for (annotationMirror in element.annotationMirrors) {
            if (annotationMirror.isOfAnnotationType<Instance>()) {
                for (entry in annotationMirror.elementValues.entries) {
                    val entryName = entry.key.simpleName.toString()
                    val entryValue = entry.value.value.toString()

                    when (entryName) {
                        ATTR_TYPE -> {
                            env.elements.getTypeElement(entryValue)?.let {
                                if (verifyInheritance) it.verifyInheritance(element)
                                interfaceTypeElement = it
                            }
                        }
                        ATTR_TYPES -> {
                            entry.value.accept(typesAttrExtractor, null)
                            interfaceTypeElements = typesAttrExtractor.extractValue()
                            if (verifyInheritance) {
                                for (typeElement in interfaceTypeElements) {
                                    if (verifyInheritance) typeElement.verifyInheritance(element)
                                }
                            }
                        }
                        ATTR_SCOPING -> scoping = entryValue
                        ATTR_CLASSIFIER -> classifier = entryValue
                        ATTR_DISABLED -> disabled = entryValue.toBoolean()

                        selectorAttributeParser.attrName -> selector =
                            selectorAttributeParser.parse(
                                value = entry.value,
                                element = element
                            )

                        factoryAttrParser.attrName -> factory =
                            factoryAttrParser.parse(
                                value = entry.value,
                                element = element
                            )

                        disposerAttrParser.attrName -> disposer =
                            disposerAttrParser.parse(
                                value = entry.value,
                                element = element
                            )
                    }
                }
            }
        }

        val declaredTypeElements: List<TypeElement> =
            verifyTypeDeclaration(interfaceTypeElement, interfaceTypeElements, scoping, element)

        return Annotation(
            types = declaredTypeElements.map { ClassName.get(it) },
            classifier = classifier,
            scoping = scoping,
            selector = selector,
            factory = factory,
            disposer = disposer,
            disabled = disabled
        ).also {
            for (validator in validators) {
                validator.validate(annotation = it, element = element)
            }
        }
    }

    private fun resolveWildcardParameterType(paramTypeName: TypeName, element: Element): TypeName {
        if (paramTypeName is WildcardTypeName) {
            if (paramTypeName.lowerBounds.size > 0) {
                throw ValidationException(
                    element = element,
                    message = "Magnet supports single upper bounds class parameter only," +
                        " while lower bounds class parameter was found."
                )
            }

            val upperBounds = paramTypeName.upperBounds
            if (upperBounds.size > 1) {
                throw ValidationException(
                    element = element,
                    message = "Magnet supports single upper bounds class parameter only," +
                        " for example List<${upperBounds[0]}>"
                )
            }

            return upperBounds[0]
        }
        return paramTypeName
    }

    private fun verifyTypeDeclaration(
        interfaceTypeElement: TypeElement?,
        interfaceTypeElements: List<TypeElement>?,
        scoping: String,
        element: Element
    ): List<TypeElement> {
        val isTypeDeclared = interfaceTypeElement != null
        val areTypesDeclared = interfaceTypeElements?.isNotEmpty() ?: false

        if (!isTypeDeclared && !areTypesDeclared) {
            throw ValidationException(
                element = element,
                message = "${Instance::class.java} must declare either 'type' or 'types' property."
            )
        }

        if (isTypeDeclared && areTypesDeclared) {
            throw ValidationException(
                element = element,
                message = "${Instance::class.java} must declare either 'type' or 'types' property, not both."
            )
        }

        if (interfaceTypeElement != null) {
            return arrayListOf(interfaceTypeElement)
        }

        if (interfaceTypeElements != null) {
            if (scoping == Scoping.UNSCOPED.name) {
                throw ValidationException(
                    element = element,
                    message = "types() property must be used with scoped instances only. Set " +
                        "scoping to Scoping.DIRECT or Scoping.TOPMOST."
                )
            }
            return interfaceTypeElements
        }

        throw CompilationException(element = element, message = "Cannot verify type declaration.")
    }

    private fun TypeElement.verifyInheritance(element: Element) {
        val isTypeImplemented = env.types.isAssignable(
            element.asType(),
            env.types.getDeclaredType(this) // erase generic type
        )
        if (!isTypeImplemented) {
            throw ValidationException(
                element = element, message = "$element must implement $this"
            )
        }
    }

    abstract fun parse(element: E): List<FactoryType>
}

internal class TypesAttrExtractor(private val elements: Elements)
    : SimpleAnnotationValueVisitor6<Void?, Void>() {

    private val extractedTypes = mutableListOf<String>()

    fun extractValue(): List<TypeElement> {
        val value = extractedTypes.map { elements.getTypeElement(it) }
        extractedTypes.clear()
        return value
    }

    override fun visitArray(values: MutableList<out AnnotationValue>?, p: Void?): Void? {
        values?.let { for (value in values) value.accept(this, p) }
        return p
    }

    override fun visitType(typeMirror: TypeMirror?, p: Void?): Void? {
        typeMirror?.let { extractedTypes.add(it.toString()) }
        return p
    }

}
