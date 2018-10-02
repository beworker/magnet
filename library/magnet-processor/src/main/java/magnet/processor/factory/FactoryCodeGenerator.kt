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
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import magnet.Classifier
import magnet.Scope
import magnet.Scoping
import magnet.internal.InstanceFactory
import javax.lang.model.element.Modifier

class FactoryCodeGenerator : FactoryTypeVisitor, CodeGenerator {

    private var factoryTypeSpec: TypeSpec? = null
    private var factoryClassName: ClassName? = null

    private lateinit var createMethodCodeBuilder: CodeBlock.Builder
    private var shouldSuppressUncheckedWarning = false
    private var constructorParametersBuilder = StringBuilder()
    private var getScoping: MethodSpec? = null

    override fun visitEnter(factoryType: FactoryType) {
        // nop
    }

    override fun visitEnter(createMethod: CreateMethod) {
        shouldSuppressUncheckedWarning = false
        factoryTypeSpec = null
        getScoping = null
        createMethodCodeBuilder = CodeBlock.builder()
        constructorParametersBuilder.setLength(0)
    }

    override fun visit(parameter: MethodParameter) {
        val isScopeParameter = parameter.name == PARAM_SCOPE_NAME

        if (!isScopeParameter) {
            if (parameter.classifier == Classifier.NONE) {
                if (parameter.method == GetterMethod.GET_MANY) {
                    if (parameter.typeErased) {
                        createMethodCodeBuilder.addStatement(
                            "\$T ${parameter.name} = scope.${parameter.method.code}(\$T.class)",
                            List::class.java,
                            parameter.type
                        )
                    } else {
                        createMethodCodeBuilder.addStatement(
                            "\$T<\$T> ${parameter.name} = scope.${parameter.method.code}(\$T.class)",
                            List::class.java,
                            parameter.type,
                            parameter.type
                        )
                    }

                } else {
                    createMethodCodeBuilder.addStatement(
                        "\$T ${parameter.name} = scope.${parameter.method.code}(\$T.class)",
                        parameter.type,
                        parameter.type
                    )
                }

            } else {
                if (parameter.method == GetterMethod.GET_MANY) {
                    if (parameter.typeErased) {
                        createMethodCodeBuilder.addStatement(
                            "\$T ${parameter.name} = scope.${parameter.method.code}(\$T.class, \$S)",
                            List::class.java,
                            parameter.type,
                            parameter.classifier
                        )
                    } else {
                        createMethodCodeBuilder.addStatement(
                            "\$T<\$T> ${parameter.name} = scope.${parameter.method.code}(\$T.class, \$S)",
                            List::class.java,
                            parameter.type,
                            parameter.type,
                            parameter.classifier
                        )
                    }
                } else {
                    createMethodCodeBuilder.addStatement(
                        "\$T ${parameter.name} = scope.${parameter.method.code}(\$T.class, \$S)",
                        parameter.type,
                        parameter.type,
                        parameter.classifier
                    )
                }
            }
        }

        if (parameter.typeErased) {
            shouldSuppressUncheckedWarning = true
        }

        constructorParametersBuilder.append(parameter.name).append(", ")
    }

    override fun visitExit(createMethod: CreateMethod) {
        if (constructorParametersBuilder.isNotEmpty()) {
            constructorParametersBuilder.setLength(constructorParametersBuilder.length - 2)
        }
    }

    override fun visit(method: GetScopingMethod) {
        getScoping = MethodSpec
            .methodBuilder("getScoping")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override::class.java)
            .returns(Scoping::class.java)
            .addStatement("return \$T.\$L", Scoping::class.java, method.scoping)
            .build()
    }

    override fun visitExit(factory: FactoryType) {
        factoryClassName = factory.factoryType
        factoryTypeSpec = TypeSpec
            .classBuilder(factoryClassName)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addSuperinterface(generateFactorySuperInterface(factory))
            .addMethod(generateCreateMethod(factory))
            .addMethod(generateGetScopingMethod())
            .addMethod(generateGetTypeMethod(factory))
            .build()
    }

    private fun generateCreateMethod(factoryType: FactoryType): MethodSpec {
        var builder = MethodSpec
            .methodBuilder("create")
            .addAnnotation(Override::class.java)
            .addModifiers(Modifier.PUBLIC)
            .addParameter(
                ParameterSpec
                    .builder(Scope::class.java, PARAM_SCOPE_NAME)
                    .build()
            )
            .returns(factoryType.annotation.type)
            .addCode(createMethodCodeBuilder.build())

        if (shouldSuppressUncheckedWarning) {
            builder = builder.addAnnotation(
                AnnotationSpec
                    .builder(SuppressWarnings::class.java)
                    .addMember("value", "\"unchecked\"")
                    .build()
            )
        }

        val createStatement = factoryType.createStatement
        when (createStatement) {
            is TypeCreateStatement -> {
                builder.addStatement(
                    "return new \$T($constructorParametersBuilder)",
                    createStatement.instanceType
                )
            }
            is MethodCreateStatement -> {
                builder.addStatement(
                    "return \$T.\$L($constructorParametersBuilder)",
                    createStatement.staticMethodClassName,
                    createStatement.staticMethodName
                )
            }
        }

        return builder.build()
    }

    private fun generateGetScopingMethod(): MethodSpec {
        return getScoping!!
    }

    private fun generateGetTypeMethod(factoryType: FactoryType): MethodSpec {
        return MethodSpec
            .methodBuilder("getType")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(Class::class.java)
            .addStatement("return \$T.class", factoryType.annotation.type)
            .build()
    }

    private fun generateFactorySuperInterface(factoryType: FactoryType): TypeName {
        return ParameterizedTypeName.get(
            ClassName.get(InstanceFactory::class.java),
            factoryType.annotation.type
        )
    }

    override fun generateFrom(factoryType: FactoryType): CodeWriter {
        factoryType.accept(this)
        return CodeWriter(this.factoryClassName!!.packageName(), factoryTypeSpec!!)
    }

}
