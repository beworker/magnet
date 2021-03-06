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

package magnet.processor

import com.google.testing.compile.CompilationSubject.assertThat
import com.google.testing.compile.Compiler
import com.google.testing.compile.JavaFileObjects
import org.junit.Test
import javax.tools.JavaFileObject

class MagnetProcessorTest {

    @Test
    fun generateFactory_NoParams() {

        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(
                withResource("HomePageNoParams.java"),
                withResource("Page.java")
            )

        assertThat(compilation).succeededWithoutWarnings()

        assertThat(compilation)
            .generatedSourceFile("app/extension/HomePageNoParamsMagnetFactory")
            .hasSourceEquivalentTo(withResource("generated/HomePageNoParamsMagnetFactory.java"))
    }

    @Test
    fun generateFactory_WithScope() {

        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(
                withResource("HomePageWithScope.java"),
                withResource("Page.java")
            )

        assertThat(compilation).succeededWithoutWarnings()

        assertThat(compilation)
            .generatedSourceFile("app/extension/HomePageWithScopeMagnetFactory")
            .hasSourceEquivalentTo(withResource("generated/HomePageWithScopeMagnetFactory.java"))
    }

    @Test
    fun generateFactory_WithArbitraryParams() {

        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(
                withResource("HomePageWithParams.java"),
                withResource("Page.java"),
                withResource("HomeRepository.java"),
                withResource("UserData.java")
            )

        assertThat(compilation).succeededWithoutWarnings()

        assertThat(compilation)
            .generatedSourceFile("app/extension/HomePageWithParamsMagnetFactory")
            .hasSourceEquivalentTo(withResource("generated/HomePageWithParamsMagnetFactory.java"))
    }

    @Test
    fun generateFactory_WithArbitraryParamsAndScope() {

        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(
                withResource("HomePage.java"),
                withResource("Page.java"),
                withResource("HomeRepository.java"),
                withResource("UserData.java")
            )

        assertThat(compilation).succeededWithoutWarnings()

        assertThat(compilation)
            .generatedSourceFile("app/extension/HomePageMagnetFactory")
            .hasSourceEquivalentTo(withResource("generated/HomePageMagnetFactory.java"))
    }

    @Test
    fun generateFactory_FailsOnGenericTypeInConstructorParameter() {

        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(
                withResource("HomePageWithGenericParam.java"),
                withResource("Page.java")
            )

        assertThat(compilation).failed()
        assertThat(compilation).hadErrorContaining("is specified using a generic type")
    }

    @Test
    fun generateFactory_TypeNotImplemented() {

        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(
                withResource("Tab.java"),
                withResource("UnimplementedTab.java")
            )

        assertThat(compilation).failed()
        assertThat(compilation).hadErrorContaining("must implement")
    }

    @Test
    fun generateFactory_DisabledAnnotation() {

        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(
                withResource("Tab.java"),
                withResource("DisabledTab.java")
            )

        assertThat(compilation).succeeded()
        com.google.common.truth.Truth.assertThat(compilation.generatedFiles().size).isEqualTo(2)
    }

    @Test
    fun generateFactory_WithClassifierParams() {

        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(
                withResource("HomePageWithClassifierParams.java"),
                withResource("Page.java"),
                withResource("HomeRepository.java"),
                withResource("UserData.java")
            )

        assertThat(compilation).succeededWithoutWarnings()

        assertThat(compilation)
            .generatedSourceFile("app/extension/HomePageWithClassifierParamsMagnetFactory")
            .hasSourceEquivalentTo(withResource("generated/HomePageWithClassifierParamsMagnetFactory.java"))
    }

    @Test
    fun generateFactory_WithManyParams() {

        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(
                withResource("HomePageWithManyParams.java"),
                withResource("Page.java"),
                withResource("HomeRepository.java")
            )

        assertThat(compilation).succeededWithoutWarnings()

        assertThat(compilation)
            .generatedSourceFile("app/extension/HomePageWithManyParamsMagnetFactory")
            .hasSourceEquivalentTo(withResource("generated/HomePageWithManyParamsMagnetFactory.java"))
    }

    @Test
    fun generateFactory_WithManyParameterizedParams() {

        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(
                withResource("HomePageWithManyParameterizedParams.java"),
                withResource("Page.java"),
                withResource("WorkProcessor.java")
            )

        assertThat(compilation).succeededWithoutWarnings()

        assertThat(compilation)
            .generatedSourceFile("app/extension/HomePageWithManyParameterizedParamsMagnetFactory")
            .hasSourceEquivalentTo(withResource("generated/HomePageWithManyParameterizedParamsMagnetFactory.java"))
    }

    @Test
    fun generateFactory_WithManyParameterizedWildcardOutParams() {

        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(
                withResource("HomePageWithManyParameterizedWildcardOutParams.java"),
                withResource("Page.java"),
                withResource("WorkProcessor.java")
            )

        assertThat(compilation).succeededWithoutWarnings()

        assertThat(compilation)
            .generatedSourceFile("app/extension/HomePageWithManyParameterizedWildcardOutParamsMagnetFactory")
            .hasSourceEquivalentTo(withResource("generated/HomePageWithManyParameterizedWildcardOutParamsMagnetFactory.java"))
    }

    @Test
    fun generateFactory_WithManyParameterizedWildcardInParams() {

        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(
                withResource("HomePageWithManyParameterizedWildcardInParams.java"),
                withResource("Page.java"),
                withResource("WorkProcessor.java")
            )

        assertThat(compilation).succeededWithoutWarnings()

        assertThat(compilation)
            .generatedSourceFile("app/extension/HomePageWithManyParameterizedWildcardInParamsMagnetFactory")
            .hasSourceEquivalentTo(withResource("generated/HomePageWithManyParameterizedWildcardInParamsMagnetFactory.java"))
    }

    @Test
    fun generateFactory_WithManyParameterizedWildcardKnownParams() {

        // This is what Kotlin provides to annotation processor, when Kotlin generics are used as parameters

        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(
                withResource("HomePageWithManyParameterizedWildcardKnownParams.java"),
                withResource("Page.java"),
                withResource("WorkProcessor.java")
            )

        assertThat(compilation).succeededWithoutWarnings()

        assertThat(compilation)
            .generatedSourceFile("app/extension/HomePageWithManyParameterizedWildcardKnownParamsMagnetFactory")
            .hasSourceEquivalentTo(withResource("generated/HomePageWithManyParameterizedWildcardKnownParamsMagnetFactory.java"))
    }

    @Test
    fun generateFactory_WithManyWildcardParams() {

        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(
                withResource("HomePageWithManyWildcardParams.java"),
                withResource("Page.java"),
                withResource("HomeRepository.java")
            )

        assertThat(compilation).succeededWithoutWarnings()

        assertThat(compilation)
            .generatedSourceFile("app/extension/HomePageWithManyWildcardParamsMagnetFactory")
            .hasSourceEquivalentTo(withResource("generated/HomePageWithManyWildcardParamsMagnetFactory.java"))
    }

    @Test
    fun generateFactory_UsingStaticMethod() {

        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(
                withResource("HomePageWithStaticConstructor.java"),
                withResource("HomePageWithStaticConstructorSingle.java"),
                withResource("Page.java"),
                withResource("HomeRepository.java")
            )

        assertThat(compilation).succeededWithoutWarnings()

        assertThat(compilation)
            .generatedSourceFile("app/extension/utils/HomePageWithStaticConstructorSingleCreateRepositoriesMagnetFactory")
            .hasSourceEquivalentTo(withResource("generated/HomePageWithStaticConstructorSingleCreateRepositoriesMagnetFactory.java"))
    }

    @Test
    fun generateFactory_StaticMethodProvidesInnerClass() {

        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(
                withResource("StaticMethodProvidesInnerClass/PowerManager.java"),
                withResource("StaticMethodProvidesInnerClass/PowerManagerProvider.java")
            )

        assertThat(compilation).succeededWithoutWarnings()
        assertThat(compilation)
            .generatedSourceFile("app/PowerManagerProviderProvideWakeLockMagnetFactory")
            .hasSourceEquivalentTo(withResource("StaticMethodProvidesInnerClass/expected/PowerManagerProviderProvideWakeLockMagnetFactory.java"))
    }

    @Test
    fun generateFactory_Covariance_Constructor_ManyParameter() {

        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(
                withResource("Covariance_Constructor_ManyParameter/Foo.java"),
                withResource("Covariance_Constructor_ManyParameter/UnderTest.java")
            )

        assertThat(compilation).succeededWithoutWarnings()
        assertThat(compilation)
            .generatedSourceFile("app/UnderTestMagnetFactory")
            .hasSourceEquivalentTo(withResource("Covariance_Constructor_ManyParameter/expected/UnderTestMagnetFactory.java"))
    }

    @Test
    fun generateFactory_Covariance_Constructor_SingleParameter() {

        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(
                withResource("Covariance_Constructor_SingleParameter/Foo.java"),
                withResource("Covariance_Constructor_SingleParameter/UnderTest.java")
            )

        assertThat(compilation).failed()
        assertThat(compilation).hadErrorContaining("is specified using a generic type")
    }

    @Test
    fun generateFactory_Lazy_Constructor_NoKotlinMetadata() {

        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(withResource("Lazy_Constructor_NoKotlinMetadata/UnderTest.java"))

        assertThat(compilation).failed()
        assertThat(compilation).hadErrorContaining("can only be used with Kotlin classes")
    }

    @Test
    fun generateFactory_Lazy_Constructor_OptionalParameter() {

        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(withResource("Lazy_Constructor_OptionalParameter/UnderTest.java"))

        assertThat(compilation).succeededWithoutWarnings()
        assertThat(compilation)
            .generatedSourceFile("app/UnderTestMagnetFactory")
            .hasSourceEquivalentTo(withResource("Lazy_Constructor_OptionalParameter/expected/UnderTestMagnetFactory.java"))
    }

    @Test
    fun generateFactory_Lazy_Constructor_OptionalParameter_Wildcard() {
        val root = "Lazy_Constructor_OptionalParameter_Wildcard"
        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(withResource("$root/UnderTest.java"))

        assertThat(compilation).succeededWithoutWarnings()
        assertThat(compilation)
            .generatedSourceFile("app/UnderTestMagnetFactory")
            .hasSourceEquivalentTo(withResource("$root/expected/UnderTestMagnetFactory.java"))
    }

    @Test
    fun generateFactory_Lazy_Constructor_SingleParameter() {

        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(withResource("Lazy_Constructor_SingleParameter/UnderTest.java"))

        assertThat(compilation).succeededWithoutWarnings()
        assertThat(compilation)
            .generatedSourceFile("app/UnderTestMagnetFactory")
            .hasSourceEquivalentTo(withResource("Lazy_Constructor_SingleParameter/expected/UnderTestMagnetFactory.java"))
    }

    @Test
    fun generateFactory_Lazy_Constructor_SingleParameter_Wildcard() {
        val root = "Lazy_Constructor_SingleParameter_Wildcard"
        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(withResource("$root/UnderTest.java"))

        assertThat(compilation).succeededWithoutWarnings()
        assertThat(compilation)
            .generatedSourceFile("app/UnderTestMagnetFactory")
            .hasSourceEquivalentTo(withResource("$root/expected/UnderTestMagnetFactory.java"))
    }

    @Test
    fun generateFactory_Lazy_Constructor_ManyParameter() {

        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(withResource("Lazy_Constructor_ManyParameter/UnderTest.java"))

        assertThat(compilation).succeededWithoutWarnings()
        assertThat(compilation)
            .generatedSourceFile("app/UnderTestMagnetFactory")
            .hasSourceEquivalentTo(withResource("Lazy_Constructor_ManyParameter/expected/UnderTestMagnetFactory.java"))
    }

    @Test
    fun generateFactory_Lazy_Constructor_ManyParameter_Wildcard() {
        val root = "Lazy_Constructor_ManyParameter_Wildcard"
        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(withResource("$root/UnderTest.java"))

        assertThat(compilation).succeededWithoutWarnings()
        assertThat(compilation)
            .generatedSourceFile("app/UnderTestMagnetFactory")
            .hasSourceEquivalentTo(withResource("$root/expected/UnderTestMagnetFactory.java"))
    }

    @Test
    fun generateFactory_Lazy_Constructor_ManyParameter_NullableGenericType() {

        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(withResource("Lazy_Constructor_ManyParameter_NullableGenericType/UnderTest.java"))

        assertThat(compilation).failed()
        assertThat(compilation).hadErrorContaining("be parametrized with none nullable type")
    }

    @Test
    fun generateFactory_Lazy_Constructor_ManyParameter_NullableListType() {

        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(withResource("Lazy_Constructor_ManyParameter_NullableListType/UnderTest.java"))

        assertThat(compilation).failed()
        assertThat(compilation).hadErrorContaining("be parametrized with none nullable List type")
    }

    @Test
    fun generateFactory_Lazy_Method_SingleParameter() {

        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(withResource("Lazy_Method_SingleParameter/UnderTest.java"))

        assertThat(compilation).succeededWithoutWarnings()
        assertThat(compilation)
            .generatedSourceFile("app/UnderTestProvideUnderTestDepMagnetFactory")
            .hasSourceEquivalentTo(withResource("Lazy_Method_SingleParameter/expected/UnderTestProvideUnderTestDepMagnetFactory.java"))
    }

    @Test
    fun generateFactory_Lazy_Method_SingleParameter_Wildcard() {
        val root = "Lazy_Method_SingleParameter_Wildcard"
        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(withResource("$root/UnderTest.java"))

        assertThat(compilation).succeededWithoutWarnings()
        assertThat(compilation)
            .generatedSourceFile("app/UnderTestProvideUnderTestDepMagnetFactory")
            .hasSourceEquivalentTo(withResource("$root/expected/UnderTestProvideUnderTestDepMagnetFactory.java"))
    }

    @Test
    fun generateFactory_Generics_ProvideTypeWithParameter() {

        val path = "Generics_ProvideTypeWithParameter"
        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(
                withResource("$path/UnderTest.java"),
                withResource("$path/Type.java"),
                withResource("$path/Parameter.java")
            )

        assertThat(compilation).succeededWithoutWarnings()
        assertThat(compilation)
            .generatedSourceFile("app/UnderTestProvideTypeMagnetFactory")
            .hasSourceEquivalentTo(withResource("$path/expected/UnderTestProvideTypeMagnetFactory.java"))
    }

    @Test
    fun generateFactory_Generics_ProvideTypeWithParameter_NoClassifier() {

        val path = "Generics_ProvideTypeWithParameter_NoClassifier"
        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(
                withResource("$path/UnderTest.java"),
                withResource("$path/Type.java"),
                withResource("$path/Parameter.java")
            )

        assertThat(compilation).failed()
        assertThat(compilation).hadErrorContaining("must have 'classifier' value")
    }

    @Test
    fun generateFactory_Lazy_Constructor_SingleParameter_ParameterizedType() {

        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(
                withResource("Lazy_Constructor_SingleParameter_ParameterizedType/Foo.java"),
                withResource("Lazy_Constructor_SingleParameter_ParameterizedType/UnderTest.java")
            )

        assertThat(compilation).succeededWithoutWarnings()
        assertThat(compilation)
            .generatedSourceFile("app/UnderTestMagnetFactory")
            .hasSourceEquivalentTo(withResource("Lazy_Constructor_SingleParameter_ParameterizedType/expected/UnderTestMagnetFactory.java"))
    }

    @Test
    fun generateFactory_Lazy_Method_OptionalParameter() {

        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(withResource("Lazy_Method_OptionalParameter/UnderTest.java"))

        assertThat(compilation).succeededWithoutWarnings()
        assertThat(compilation)
            .generatedSourceFile("app/UnderTestProvideUnderTestDepMagnetFactory")
            .hasSourceEquivalentTo(withResource("Lazy_Method_OptionalParameter/expected/UnderTestProvideUnderTestDepMagnetFactory.java"))
    }

    @Test
    fun generateFactory_Lazy_Method_OptionalParameter_Wildcard() {
        val root = "Lazy_Method_OptionalParameter_Wildcard"
        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(withResource("$root/UnderTest.java"))

        assertThat(compilation).succeededWithoutWarnings()
        assertThat(compilation)
            .generatedSourceFile("app/UnderTestProvideUnderTestDepMagnetFactory")
            .hasSourceEquivalentTo(withResource("$root/expected/UnderTestProvideUnderTestDepMagnetFactory.java"))
    }

    @Test
    fun generateFactory_Limit_NotEmpty_GenerateGetter() {
        val root = "Limit_NotEmpty_HasGetter"
        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(withResource("$root/UnderTest.java"))

        assertThat(compilation).succeededWithoutWarnings()
        assertThat(compilation)
            .generatedSourceFile("app/UnderTestMagnetFactory")
            .hasSourceEquivalentTo(withResource("$root/expected/UnderTestMagnetFactory.java"))
    }

    @Test
    fun generateFactory_Limit_Empty_NoGetter() {
        val root = "Limit_Empty_NoGetter"
        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(withResource("$root/UnderTest.java"))

        assertThat(compilation).succeededWithoutWarnings()
        assertThat(compilation)
            .generatedSourceFile("app/UnderTestMagnetFactory")
            .hasSourceEquivalentTo(withResource("$root/expected/UnderTestMagnetFactory.java"))
    }

    @Test
    fun generateFactory_Limit_ReservedAsterix_Fails() {
        val root = "Limit_ReservedAsterisks_Fails"
        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(withResource("$root/UnderTest.java"))

        assertThat(compilation).failed()
        assertThat(compilation).hadErrorContaining("Use another value")
    }

    @Test
    fun generateFactory_Limit_ScopingDirect_GeneratesGetter() {
        val root = "Limit_ScopingDirect_GeneratesGetter"
        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(withResource("$root/UnderTest.java"))

        assertThat(compilation).succeededWithoutWarnings()
        assertThat(compilation)
            .generatedSourceFile("app/UnderTestMagnetFactory")
            .hasSourceEquivalentTo(withResource("$root/expected/UnderTestMagnetFactory.java"))
    }

    @Test
    fun generateFactory_Limit_ScopingUnscoped_Fails() {
        val root = "Limit_ScopingUnscoped_Fails"
        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(withResource("$root/UnderTest.java"))

        assertThat(compilation).failed()
        assertThat(compilation).hadErrorContaining("Limit can only be used with Scoping.TOPMOST")
    }

    @Test
    fun generateFactory_Lazy_Method_NoKotlinMetadata() {

        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(withResource("Lazy_Method_NoKotlinMetadata/UnderTest.java"))

        assertThat(compilation).failed()
        assertThat(compilation).hadErrorContaining("can only be used with Kotlin classes")
    }

    @Test
    fun generateFactory_ScopeParameter_CustomName() {

        val path = "ScopeParameter_CustomName"
        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(
                withResource("$path/UnderTest.java")
            )

        assertThat(compilation).succeededWithoutWarnings()
        assertThat(compilation)
            .generatedSourceFile("app/UnderTestMagnetFactory")
            .hasSourceEquivalentTo(withResource("$path/expected/UnderTestMagnetFactory.java"))
    }

    @Test
    fun generateFactory_ScopeParameter_CustomName_KotlinClass() {

        val path = "ScopeParameter_CustomName_KotlinClass"
        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(
                withResource("$path/UnderTest.java")
            )

        assertThat(compilation).succeededWithoutWarnings()
        assertThat(compilation)
            .generatedSourceFile("app/UnderTestMagnetFactory")
            .hasSourceEquivalentTo(withResource("$path/expected/UnderTestMagnetFactory.java"))
    }

    @Test
    fun generateFactory_ScopeParameter_DefaultName() {

        val path = "ScopeParameter_DefaultName"
        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(
                withResource("$path/UnderTest.java")
            )

        assertThat(compilation).succeededWithoutWarnings()
        assertThat(compilation)
            .generatedSourceFile("app/UnderTestMagnetFactory")
            .hasSourceEquivalentTo(withResource("$path/expected/UnderTestMagnetFactory.java"))
    }

    @Test
    fun generateFactory_DefaultArguments_JvmOverloads_AtTheEnd() {

        val path = "DefaultArguments_JvmOverloads_AtTheEnd"
        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(
                withResource("$path/UnderTest.java")
            )

        assertThat(compilation).succeededWithoutWarnings()
        assertThat(compilation)
            .generatedSourceFile("app/UnderTestMagnetFactory")
            .hasSourceEquivalentTo(withResource("$path/expected/UnderTestMagnetFactory.java"))
    }

    @Test
    fun generateFactory_DefaultArguments_JvmOverloads_InTheMiddle() {

        val path = "DefaultArguments_JvmOverloads_InTheMiddle"
        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(
                withResource("$path/UnderTest.java")
            )

        assertThat(compilation).succeededWithoutWarnings()
        assertThat(compilation)
            .generatedSourceFile("app/UnderTestMagnetFactory")
            .hasSourceEquivalentTo(withResource("$path/expected/UnderTestMagnetFactory.java"))
    }

    @Test
    fun generateFactory_DefaultArguments_JvmOverloads_Mixed() {

        val path = "DefaultArguments_JvmOverloads_Mixed"
        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(
                withResource("$path/UnderTest.java")
            )

        assertThat(compilation).succeededWithoutWarnings()
        assertThat(compilation)
            .generatedSourceFile("app/UnderTestMagnetFactory")
            .hasSourceEquivalentTo(withResource("$path/expected/UnderTestMagnetFactory.java"))
    }

    @Test
    fun generateFactory_StaticMethodNeedsDependencyWithClassifier() {
        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(
                withResource("StaticMethodNeedsDependencyWithClassifier/Constants.java"),
                withResource("StaticMethodNeedsDependencyWithClassifier/Input.java"),
                withResource("StaticMethodNeedsDependencyWithClassifier/Output.java"),
                withResource("StaticMethodNeedsDependencyWithClassifier/StaticFunction.java")
            )

        assertThat(compilation).succeededWithoutWarnings()
        assertThat(compilation)
            .generatedSourceFile("app/StaticFunctionProvideInputMagnetFactory")
            .hasSourceEquivalentTo(withResource("StaticMethodNeedsDependencyWithClassifier/generated/StaticFunctionProvideInputMagnetFactory.java"))
    }

    @Test
    fun generateFactory_DisabledAnnotation_UsingStaticMethod() {

        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(
                withResource("HomePageWithStaticConstructor.java"),
                withResource("HomePageWithStaticConstructorDisabled.java"),
                withResource("Page.java"),
                withResource("HomeRepository.java")
            )

        assertThat(compilation).succeededWithoutWarnings()
        com.google.common.truth.Truth.assertThat(compilation.generatedFiles().size).isEqualTo(4)
    }

    @Test
    fun generateFactoryIndex_ForInterfaceWithGenericType() {

        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(
                withResource("Executor.java"),
                withResource("ExecutorImpl.java"),
                withResource("AppExtensionRegistry.java")
            )

        assertThat(compilation)
            .generatedSourceFile("app/extension/ExecutorImplMagnetFactory")
            .hasSourceEquivalentTo(withResource("generated/ForInterfaceWithGenericType_ExecutorMagnetFactory.java"))
    }

    @Test
    fun generateFactory_Constructor_Public_PackagePrivate() {

        val path = "Constructor_Public_PackagePrivate"
        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(
                withResource("$path/UnderTest.java")
            )

        assertThat(compilation).failed()
        assertThat(compilation).hadErrorContaining("must have exactly one public or package-private constructor")
    }

    @Test
    fun generateFactory_Constructor_Public_Public() {

        val path = "Constructor_Public_Public"
        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(
                withResource("$path/UnderTest.java")
            )

        assertThat(compilation).failed()
        assertThat(compilation).hadErrorContaining("must have exactly one public or package-private constructor")
    }

    @Test
    fun generateFactory_Constructor_Public_Protected() {

        val path = "Constructor_Public_Protected"
        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(
                withResource("$path/UnderTest.java")
            )

        assertThat(compilation).succeededWithoutWarnings()
        assertThat(compilation)
            .generatedSourceFile("app/UnderTestMagnetFactory")
            .hasSourceEquivalentTo(withResource("$path/expected/UnderTestMagnetFactory.java"))
    }

    @Test
    fun generateFactory_Constructor_Public_Private() {

        val path = "Constructor_Public_Private"
        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(
                withResource("$path/UnderTest.java")
            )

        assertThat(compilation).succeededWithoutWarnings()
        assertThat(compilation)
            .generatedSourceFile("app/UnderTestMagnetFactory")
            .hasSourceEquivalentTo(withResource("$path/expected/UnderTestMagnetFactory.java"))
    }

    @Test
    fun generateFactory_Constructor_PackagePrivate_PackagePrivate() {

        val path = "Constructor_PackagePrivate_PackagePrivate"
        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(
                withResource("$path/UnderTest.java")
            )

        assertThat(compilation).failed()
        assertThat(compilation).hadErrorContaining("must have exactly one public or package-private constructor")
    }

    @Test
    fun generateFactory_Constructor_Private() {

        val path = "Constructor_Private"
        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(
                withResource("$path/UnderTest.java")
            )

        assertThat(compilation).failed()
        assertThat(compilation).hadErrorContaining("must have exactly one public or package-private constructor")
    }

    @Test
    fun generateFactory_Constructor_Protected() {

        val path = "Constructor_Protected"
        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(
                withResource("$path/UnderTest.java")
            )

        assertThat(compilation).failed()
        assertThat(compilation).hadErrorContaining("must have exactly one public or package-private constructor")
    }

    @Test
    fun generateFactory_Constructor_PackagePrivate_Private() {

        val path = "Constructor_PackagePrivate_Private"
        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(
                withResource("$path/UnderTest.java")
            )

        assertThat(compilation).succeededWithoutWarnings()

        assertThat(compilation)
            .generatedSourceFile("app/UnderTestMagnetFactory")
            .hasSourceEquivalentTo(withResource("$path/expected/UnderTestMagnetFactory.java"))
    }

    @Test
    fun generateFactory_Constructor_Public_Public_Kotlin() {

        val path = "Constructor_Public_Public_Kotlin"
        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(
                withResource("$path/UnderTest.java")
            )

        assertThat(compilation).failed()
        assertThat(compilation).hadErrorContaining("must have exactly one public or package-private constructor")
    }

    @Test
    fun generateFactory_Generics_GetSingle_Unchecked() {
        val path = "Generics_GetSingle_Unchecked"
        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(
                withResource("$path/Dependency.java"),
                withResource("$path/UnderTest.java")
            )

        assertThat(compilation).succeededWithoutWarnings()

        assertThat(compilation)
            .generatedSourceFile("app/UnderTestMagnetFactory")
            .hasSourceEquivalentTo(withResource("$path/expected/UnderTestMagnetFactory.java"))
    }

    @Test
    fun generateFactory_Generics_GetOptional_Unchecked() {
        val path = "Generics_GetOptional_Unchecked"
        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(
                withResource("$path/Dependency.java"),
                withResource("$path/UnderTest.java")
            )

        assertThat(compilation).succeededWithoutWarnings()

        assertThat(compilation)
            .generatedSourceFile("app/UnderTestMagnetFactory")
            .hasSourceEquivalentTo(withResource("$path/expected/UnderTestMagnetFactory.java"))
    }

    @Test
    fun generateFactory_TypeAutoDetect_ExtendsObjectNoInterfaces() {
        val path = "TypeAutoDetect_ExtendsObjectNoInterfaces"
        val compilation = Compiler.javac()
            .withProcessors(MagnetProcessor())
            .compile(
                withResource("$path/UnderTest.java")
            )

        assertThat(compilation).succeededWithoutWarnings()

        assertThat(compilation)
            .generatedSourceFile("app/UnderTestMagnetFactory")
            .hasSourceEquivalentTo(withResource("$path/expected/UnderTestMagnetFactory.java"))
    }

    private fun withResource(name: String): JavaFileObject {
        return JavaFileObjects.forResource(javaClass.simpleName + '/' + name)
    }
}
