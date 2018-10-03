package magnet.processor.factory

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.WildcardTypeName
import magnet.Classifier
import magnet.Instance
import magnet.Scope
import magnet.Scoping
import magnet.processor.MagnetProcessorEnv
import magnet.processor.mirrors
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeKind

private const val CLASS_NULLABLE = ".Nullable"
private const val ATTR_TYPE = "type"
private const val ATTR_SCOPING = "scoping"
private const val ATTR_CLASSIFIER = "classifier"
private const val ATTR_DISABLED = "disabled"

internal open class AnnotationParser(
    protected val env: MagnetProcessorEnv
) {

    protected fun parseMethodParameter(
        element: Element,
        variable: VariableElement
    ): MethodParameter {

        val variableType = variable.asType()
        if (variableType.kind == TypeKind.TYPEVAR) {
            throw env.compilationError(element,
                "Constructor parameter '${variable.simpleName}' is specified using a generic" +
                    " type which is not supported by Magnet. Use a non-parameterized class or interface" +
                    " type instead. To inject current scope instance, use 'Scope' parameter type.")
        }

        val paramSpec = ParameterSpec.get(variable)
        val paramName = paramSpec.name

        val isScopeParam = variableType.toString() == Scope::class.java.name
        if (isScopeParam) {
            return MethodParameter(
                PARAM_SCOPE_NAME,
                ClassName.get(Scope::class.java),
                false,
                Classifier.NONE,
                GetterMethod.GET_SCOPE
            )
        }

        var paramTypeName = paramSpec.type
        var getterMethod: GetterMethod? = null

        var paramTypeErased = false
        paramTypeName = if (paramTypeName is ParameterizedTypeName) {

            if (paramTypeName.rawType.reflectionName() == List::class.java.typeName) {
                getterMethod = GetterMethod.GET_MANY

                var listParamTypeName = paramTypeName.typeArguments[0]
                listParamTypeName = resolveWildcardParameterType(listParamTypeName, element)

                if (listParamTypeName is ParameterizedTypeName) {
                    if (!listParamTypeName.typeArguments.isEmpty()) {
                        paramTypeErased = true
                        listParamTypeName = listParamTypeName.rawType
                    }
                }

                listParamTypeName

            } else {
                if (!paramTypeName.typeArguments.isEmpty()) {
                    paramTypeErased = true
                }
                paramTypeName.rawType
            }

        } else {
            ClassName.get(variableType)
        }

        paramTypeName = resolveWildcardParameterType(paramTypeName, element)

        var hasNullableAnnotation = false
        var classifier: String = Classifier.NONE

        variable.annotationMirrors.forEach { annotationMirror ->
            if (annotationMirror.mirrors<Classifier>()) {
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
            paramName,
            paramTypeName,
            paramTypeErased,
            classifier,
            getterMethod
        )
    }

    protected fun parseAnnotation(element: Element, checkInheritance: Boolean = false): Annotation {

        var interfaceTypeElement: TypeElement? = null
        var scoping = Scoping.TOPMOST.name
        var classifier = Classifier.NONE
        var disabled = false

        element.annotationMirrors.forEach { annotationMirror ->
            if (annotationMirror.mirrors<Instance>()) {
                annotationMirror.elementValues.entries.forEach { entry ->
                    val entryName = entry.key.simpleName.toString()
                    val entryValue = entry.value.value.toString()
                    when (entryName) {
                        ATTR_TYPE -> {
                            interfaceTypeElement = env.elements.getTypeElement(entryValue)
                            if (checkInheritance) {
                                val isTypeImplemented = env.types.isAssignable(
                                    element.asType(),
                                    env.types.getDeclaredType(interfaceTypeElement) // erase generic type
                                )
                                if (!isTypeImplemented) {
                                    throw env.compilationError(element,
                                        "$element must implement $interfaceTypeElement")
                                }
                            }
                        }
                        ATTR_SCOPING -> scoping = entryValue
                        ATTR_CLASSIFIER -> classifier = entryValue
                        ATTR_DISABLED -> disabled = entryValue.toBoolean()
                    }
                }
            }
        }

        val interfaceType = if (interfaceTypeElement == null) {
            throw env.compilationError(element, "${Instance::class.java} must declare 'type' property.")
        } else {
            ClassName.get(interfaceTypeElement)
        }

        return Annotation(
            interfaceType,
            classifier,
            scoping,
            disabled
        )
    }

    private fun resolveWildcardParameterType(paramTypeName: TypeName, element: Element): TypeName {
        if (paramTypeName is WildcardTypeName) {
            if (paramTypeName.lowerBounds.size > 0) {
                throw env.compilationError(element,
                    "Magnet supports single upper bounds class parameter only," +
                        " while lower bounds class parameter was found.")
            }

            val upperBounds = paramTypeName.upperBounds
            if (upperBounds.size > 1) {
                throw env.compilationError(element,
                    "Magnet supports single upper bounds class parameter only," +
                        " for example List<${upperBounds[0]}>")
            }

            return upperBounds[0]
        }
        return paramTypeName
    }

}
