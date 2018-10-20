/*
 * Copyright (C) 2018 Sergej Shafarenka, www.halfbit.de
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

package magnet.processor.factory

import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeSpec
import magnet.internal.FactoryIndex
import javax.lang.model.element.Modifier

class FactoryIndexCodeGenerator : FactoryTypeVisitor, CodeGenerator {

    private var factoryIndexTypeSpec: TypeSpec? = null
    private var factoryIndexClassName: ClassName? = null

    override fun visitEnter(factoryType: FactoryType) {
        factoryIndexTypeSpec = null
    }

    override fun visitEnter(createMethod: CreateMethod) {}
    override fun visit(parameter: MethodParameter) {}
    override fun visitExit(createMethod: CreateMethod) {}
    override fun visit(method: GetScopingMethod) {}
    override fun visit(method: GetSiblingTypesMethod) {}

    override fun visitExit(factory: FactoryType) {
        val factoryPackage = factory.factoryType.packageName()
        val factoryName = factory.factoryType.simpleName()
        val factoryIndexName = "${factoryPackage.replace('.', '_')}_${factoryName}"
        factoryIndexClassName = ClassName.get("magnet.index", factoryIndexName)

        factoryIndexTypeSpec = TypeSpec
            .classBuilder(factoryIndexClassName)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addAnnotation(
                generateFactoryIndexAnnotation(
                    factory.factoryType,
                    factory.annotation.type.reflectionName(),
                    factory.annotation.classifier
                )
            )
            .build()
    }

    private fun generateFactoryIndexAnnotation(
        factoryClassName: ClassName,
        implType: String,
        implClassifier: String
    ): AnnotationSpec {
        return AnnotationSpec.builder(FactoryIndex::class.java)
            .addMember("factory", "\$T.class", factoryClassName)
            .addMember("type", "\$S", implType)
            .addMember("classifier", "\$S", implClassifier)
            .build()
    }

    override fun generateFrom(factoryType: FactoryType): CodeWriter {
        factoryType.accept(this)
        return CodeWriter(factoryIndexClassName!!.packageName(), factoryIndexTypeSpec!!)
    }

}