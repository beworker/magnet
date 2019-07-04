/*
 * Copyright (C) 2018-2019 Sergej Shafarenka, www.halfbit.de
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import magnet.processor.common.isOfAnnotationType
import magnet.processor.common.validationError
import magnet.processor.instances.disposer.DisposerAnnotationValidator
import magnet.processor.instances.disposer.DisposerAttributeParser
import magnet.processor.instances.factory.FactoryAttributeParser
import magnet.processor.instances.kotlin.MethodMeta
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
private const val ATTR_LIMIT = "limit"
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

    private val scopeTypeName = ClassName.get(Scope::class.java)
    private val listTypeName = ClassName.get(List::class.java)
    private val lazyTypeName = ClassName.get(Lazy::class.java)

    protected fun parseMethodParameter(
        element: Element,
        variable: VariableElement,
        methodMeta: MethodMeta?
    ): MethodParameter {

        val variableType = variable.asType()
        if (variableType.kind == TypeKind.TYPEVAR) {
            element.validationError(
                "Constructor parameter '${variable.simpleName}' is specified using a generic" +
                    " type which is not supported by Magnet. Use a non-parameterized class or interface" +
                    " type instead. To inject current scope instance, use 'Scope' parameter type."
            )
        }

        val paramSpec = ParameterSpec.get(variable)
        val paramType = paramSpec.type
        val paramName = paramSpec.name

        var paramReturnType: TypeName = paramType
        var paramExpression: Expression = Expression.Scope
        var paramParameterType: TypeName = paramType
        var paramClassifier: String = Classifier.NONE
        var paramTypeErased = false

        paramParameterType.parseParamType(
            paramName, methodMeta, variable
        ) { returnType, expression, parameterType, classifier, erased ->
            paramReturnType = returnType
            paramExpression = expression
            paramParameterType = parameterType
            paramClassifier = classifier
            paramTypeErased = erased
        }

        return MethodParameter(
            name = paramName,
            expression = paramExpression,
            returnType = paramReturnType,
            parameterType = paramParameterType,
            classifier = paramClassifier,
            typeErased = paramTypeErased
        )
    }

    private fun TypeName.parseParamType(
        paramName: String,
        methodMeta: MethodMeta?,
        variable: VariableElement,
        block: (
            returnType: TypeName,
            expression: Expression,
            parameterType: TypeName,
            classifier: String,
            typeErased: Boolean
        ) -> Unit
    ) {
        var paramReturnType: TypeName = this
        var paramExpression: Expression = Expression.Scope
        var paramParameterType: TypeName = this
        var paramClassifier: String = Classifier.NONE
        var paramTypeErased = false

        when (this) {
            scopeTypeName -> {
                paramExpression = Expression.Scope
            }

            is ParameterizedTypeName -> {
                when (rawType) {
                    listTypeName -> {
                        val (type, erased) = firstArgumentRawType(variable)
                        paramParameterType = type
                        paramTypeErased = erased
                        paramReturnType = if (erased) listTypeName
                        else ParameterizedTypeName.get(listTypeName, paramParameterType)
                        paramExpression = Expression.Getter(Cardinality.Many)
                        variable.annotations { _, classifier ->
                            paramClassifier = classifier
                        }
                    }

                    lazyTypeName -> {
                        if (methodMeta == null) variable.validationError(
                            "Lazy can only be used with Kotlin classes."
                        )

                        parseLazyArgumentType(
                            paramName, methodMeta, variable
                        ) { returnType, cardinality, parameterType ->
                            paramReturnType = ParameterizedTypeName.get(lazyTypeName, returnType)
                            paramParameterType = parameterType
                            paramExpression = Expression.LazyGetter(cardinality)
                            variable.annotations { _, classifier ->
                                paramClassifier = classifier
                            }
                        }
                    }

                    else -> {
                        paramParameterType = rawType
                        variable.annotations { cardinality, classifier ->
                            paramExpression = Expression.Getter(cardinality)
                            paramClassifier = classifier
                        }
                    }
                }
            }

            else -> {
                variable.annotations { cardinality, classifier ->
                    paramExpression = Expression.Getter(cardinality)
                    paramClassifier = classifier
                }
            }
        }

        block(
            paramReturnType,
            paramExpression,
            paramParameterType,
            paramClassifier,
            paramTypeErased
        )
    }

    private fun ParameterizedTypeName.parseLazyArgumentType(
        paramName: String,
        methodMeta: MethodMeta,
        variable: VariableElement,
        block: (
            returnType: TypeName,
            cardinality: Cardinality,
            parameterType: TypeName
        ) -> Unit
    ) {
        when (val argumentType = typeArguments.first().withoutWildcards(variable)) {
            scopeTypeName -> variable.validationError("Lazy cannot be parametrized with Scope type.")
            is ParameterizedTypeName -> {
                when (argumentType.rawType) {
                    lazyTypeName -> variable.validationError("Lazy cannot be parametrized with another Lazy type.")
                    listTypeName -> {
                        if (methodMeta.getTypeMeta(paramName, 1).nullable) {
                            variable.validationError(
                                "Lazy<List> must be parametrized with none nullable List type."
                            )
                        }
                        when (val listArgumentType = argumentType.typeArguments.first().withoutWildcards(variable)) {
                            is ParameterizedTypeName -> {
                                block(
                                    ParameterizedTypeName.get(listTypeName, listArgumentType),
                                    Cardinality.Many,
                                    listArgumentType.rawType
                                )
                            }
                            else -> {
                                if (methodMeta.getTypeMeta(paramName, 2).nullable) {
                                    variable.validationError(
                                        "Lazy<List<T>> must be parametrized with none nullable type."
                                    )
                                }
                                block(
                                    ParameterizedTypeName.get(listTypeName, listArgumentType),
                                    Cardinality.Many,
                                    listArgumentType
                                )
                            }
                        }
                    }
                    else -> {
                        block(
                            argumentType,
                            methodMeta.getNullableCardinality(paramName, 1),
                            argumentType.rawType
                        )
                    }
                }
            }
            else -> {
                block(
                    argumentType,
                    methodMeta.getNullableCardinality(paramName, 1),
                    argumentType
                )
            }
        }
    }

    protected fun parseAnnotation(element: Element): Annotation {

        var interfaceTypeElement: TypeElement? = null
        var interfaceTypeElements: List<TypeElement>? = null
        var scoping = Scoping.TOPMOST.name
        var classifier = Classifier.NONE
        var limit = ""
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
                        ATTR_LIMIT -> limit = entryValue
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
            verifyTypeDeclaration(
                interfaceTypeElement,
                interfaceTypeElements,
                scoping,
                limit,
                element
            )

        return Annotation(
            types = declaredTypeElements.map { ClassName.get(it) },
            classifier = classifier,
            scoping = scoping,
            limit = limit,
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

    private fun verifyTypeDeclaration(
        interfaceTypeElement: TypeElement?,
        interfaceTypeElements: List<TypeElement>?,
        scoping: String,
        limit: String,
        element: Element
    ): List<TypeElement> {
        val isTypeDeclared = interfaceTypeElement != null
        val areTypesDeclared = interfaceTypeElements?.isNotEmpty() ?: false

        if (!isTypeDeclared && !areTypesDeclared) {
            element.validationError(
                "${Instance::class.java} must declare either 'type' or 'types' property."
            )
        }

        if (isTypeDeclared && areTypesDeclared) {
            element.validationError(
                "${Instance::class.java} must declare either 'type' or 'types' property, not both."
            )
        }

        if (limit == "*") {
            element.validationError(
                "Limit must not use reserved '*' value. Use another constant."
            )
        } else if (limit.isNotEmpty() && scoping != Scoping.TOPMOST.name) {
            element.validationError(
                "Limit can only be used with Scoping.TOPMOST. Actual scoping: Scoping.$scoping"
            )
        }

        if (interfaceTypeElement != null) {
            return arrayListOf(interfaceTypeElement)
        }

        if (interfaceTypeElements != null) {
            if (scoping == Scoping.UNSCOPED.name) {
                element.validationError(
                    "types() property must be used with scoped instances only. Set " +
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
            env.types.getDeclaredType(this)
        )
        if (!isTypeImplemented) {
            element.validationError("$element must implement $this")
        }
    }

    abstract fun parse(element: E): List<FactoryType>
}

private fun MethodMeta.getNullableCardinality(paramName: String, paramDepth: Int): Cardinality =
    if (getTypeMeta(paramName, paramDepth).nullable) Cardinality.Optional
    else Cardinality.Single

private fun TypeName.withoutWildcards(element: Element): TypeName =
    if (this is WildcardTypeName) {
        checkBounds(element)
        upperBounds.first()
    } else this

private fun WildcardTypeName.firstUpperBoundsRawType(element: Element): Pair<TypeName, Boolean> {
    checkBounds(element)
    return when (val type = upperBounds.first()) {
        is ParameterizedTypeName -> type.rawType to true
        is WildcardTypeName -> type.firstUpperBoundsRawType(element)
        else -> type to false
    }
}

private fun WildcardTypeName.checkBounds(element: Element) {
    if (lowerBounds.size > 0) {
        element.validationError(
            "Magnet supports single upper bounds class parameter only," +
                " while lower bounds class parameter was found."
        )
    }

    if (upperBounds.size > 1) {
        element.validationError(
            "Magnet supports single upper bounds class parameter only," +
                " for example List<${upperBounds.first()}>"
        )
    }
}

private fun ParameterizedTypeName.firstArgumentRawType(element: Element): Pair<TypeName, Boolean> {
    if (typeArguments.size > 1) {
        element.validationError("Magnet supports type parametrized with a single argument only.")
    }
    return when (val argumentType = typeArguments.first()) {
        is ParameterizedTypeName -> argumentType.rawType to true
        is WildcardTypeName -> argumentType.firstUpperBoundsRawType(element)
        else -> argumentType to false
    }
}

private inline fun VariableElement.annotations(block: (Cardinality, String) -> Unit) {
    var cardinality = Cardinality.Single
    var classifier = Classifier.NONE
    annotationMirrors.forEach { annotationMirror ->
        if (annotationMirror.isOfAnnotationType<Classifier>()) {
            val declaredClassifier: String? = annotationMirror.elementValues.values.firstOrNull()?.value.toString()
            declaredClassifier?.let {
                classifier = it.removeSurrounding("\"", "\"")
            }
        } else {
            val annotationType = annotationMirror.annotationType.toString()
            if (annotationType.endsWith(CLASS_NULLABLE)) {
                cardinality = Cardinality.Optional
            }
        }
    }
    block(cardinality, classifier)
}

internal class TypesAttrExtractor(private val elements: Elements) :
    SimpleAnnotationValueVisitor6<Void?, Void>() {

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
