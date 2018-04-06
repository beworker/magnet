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

package magnet

import javax.annotation.processing.Filer
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.tools.Diagnostic

class MagnetProcessorEnv(private val processEnvironment: ProcessingEnvironment) {

    val filer: Filer
        get() = processEnvironment.filer

    val elements: Elements
        get() = processEnvironment.elementUtils

    val types: Types
        get() = processEnvironment.typeUtils

    fun reportError(element: Element, message: String) {
        processEnvironment.messager
            .printMessage(Diagnostic.Kind.ERROR, message, element)
    }

}