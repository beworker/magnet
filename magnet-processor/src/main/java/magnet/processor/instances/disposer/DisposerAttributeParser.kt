package magnet.processor.instances.disposer

import magnet.processor.common.AnnotationValueExtractor
import magnet.processor.common.ValidationException
import magnet.processor.instances.AttributeParser
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.type.TypeKind

internal class DisposerAttributeParser(
    private val extractor: AnnotationValueExtractor
) : AttributeParser<String> {

    override val attrName: String = "disposer"

    override fun parse(value: AnnotationValue, element: Element): String {

        if (element.kind != ElementKind.CLASS) {
            throw ValidationException(
                element = element,
                message = "Disposer can be defined for annotated class only."
            )
        }

        val methodName = extractor
            .getStringValue(value)
            .removeSurrounding("\"")

        val methodElement = element.enclosedElements
            .find { it.kind == ElementKind.METHOD && it.simpleName.toString() == methodName }
            ?: throw ValidationException(
                element = element,
                message = "Instance must declare disposer method $methodName()."
            )

        val returnType = (methodElement as ExecutableElement).returnType
        if (returnType.kind != TypeKind.VOID) {
            throw ValidationException(
                element = element,
                message = "Disposer method $methodName() must return void."
            )
        }

        if (methodElement.parameters.size != 0) {
            throw ValidationException(
                element = element,
                message = "Disposer method $methodName() must have no parameters."
            )
        }

        if (methodElement.modifiers.contains(Modifier.PRIVATE)) {
            throw ValidationException(
                element = element,
                message = "Disposer method $methodName() must not be 'private'."
            )
        }

        return methodName
    }
}