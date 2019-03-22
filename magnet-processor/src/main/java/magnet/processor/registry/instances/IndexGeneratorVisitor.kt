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

package magnet.processor.registry.instances

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import magnet.processor.registry.instances.Model.Index
import magnet.processor.registry.instances.Model.IndexVisitor
import magnet.processor.registry.instances.Model.Inst
import magnet.processor.registry.instances.Model.Range
import magnet.processor.registry.instances.Model.Section

class IndexGeneratorVisitor : IndexVisitor {

    val indexBuilder: CodeBlock.Builder = CodeBlock.builder()
    val targetsBuilder: CodeBlock.Builder = CodeBlock.builder()

    private val rangeClassName: ClassName = ClassName.bestGuess("magnet.internal.Range")
    private var generateSingleRange = false
    private var currentSection: Section? = null
    private var sectionIndex = 0

    override fun visit(inst: Inst) {
        // nop
    }

    override fun visit(index: Index) {
        // nop
    }

    override fun visit(section: Section) {

        generateSingleRange = section.ranges.size == 1
        currentSection = section

        if (generateSingleRange) {
            return
        }

        val targetsName = "ranges${++sectionIndex}"

        indexBuilder.addStatement(
            "index.put(\$T.getType(), \$L)",
            section.firstFactory,
            targetsName
        )

        val mapSize = Math.max(Math.round(section.ranges.size / .75f), 8)
        targetsBuilder.addStatement(
            "\$T<\$T, \$T> \$L = new \$T<>($mapSize)",
            Map::class.java,
            String::class.java,
            rangeClassName,
            targetsName,
            HashMap::class.java
        )
    }

    override fun visit(range: Range) {

        if (generateSingleRange) {
            currentSection?.let {
                indexBuilder.addStatement(
                    "index.put(\$T.getType(), new \$T(\$L, \$L, \$S))",
                    range.firstFactory,
                    rangeClassName,
                    range.from,
                    range.impls.size,
                    range.classifier
                )
            }
            return
        }

        val targetsName = "ranges${sectionIndex}"

        targetsBuilder.addStatement(
            "\$L.put(\$S, new \$T(\$L, \$L, \$S))",
            targetsName,
            range.classifier,
            rangeClassName,
            range.from,
            range.impls.size,
            range.classifier
        )
    }

}