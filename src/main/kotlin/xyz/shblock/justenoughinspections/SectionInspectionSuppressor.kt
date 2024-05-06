package xyz.shblock.justenoughinspections

import com.intellij.codeInspection.*
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.*
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import com.intellij.util.containers.Stack
import com.intellij.util.takeWhileInclusive
import kotlinx.collections.immutable.toImmutableList
import java.util.regex.Pattern

@Suppress("PrivatePropertyName")
class SectionInspectionSuppressor : InspectionSuppressor {
    private val PATTERN_BEGIN = Pattern.compile(".*(?:begin|Begin|BEGIN)\\s*:\\s*" + SuppressionUtil.COMMON_SUPPRESS_REGEXP)
    private val PATTERN_END = Pattern.compile(".*(?:end|End|END)\\s*:\\s*" + SuppressionUtilCore.SUPPRESS_INSPECTIONS_TAG_NAME)

    private fun getSuppressRanges(file: PsiFile): List<Pair<TextRange, String>> {
        val ranges = mutableListOf<Pair<TextRange, String>>()

        val stack = Stack<Pair<Int, String>>()
        val lastLeaf = PsiTreeUtil.lastChild(file)
        generateSequence(PsiTreeUtil.firstChild(file), PsiTreeUtil::nextLeaf)
            .takeWhileInclusive { it !== lastLeaf }
            .forEach {
                if (it is PsiComment) {
                    val text = it.text

                    val mb = PATTERN_BEGIN.matcher(text)
                    if (mb.matches()) {
                        stack.push(Pair(it.endOffset + 1, mb.group(1)))
                        return@forEach
                    }

                    val me = PATTERN_END.matcher(text)
                    if (me.matches()) {
                        if (stack.empty()) return@forEach
                        val (offset, ids) = stack.pop()
                        ranges.add(Pair(TextRange(offset, it.startOffset - 1), ids))
                        return@forEach
                    }
                }
            }

        stack.reversed().forEach {
            val (offset, ids) = it
            ranges.add(Pair(TextRange(offset, file.endOffset), ids))
        }

        return ranges.toImmutableList()
    }

    override fun isSuppressedFor(element: PsiElement, toolId: String): Boolean {
        return CachedValuesManager.getProjectPsiDependentCache(
            element.containingFile,
            ::getSuppressRanges
        ).any {
            it.first.contains(element.textRange)
                && SuppressionUtil.isInspectionToolIdMentioned(it.second, toolId)
        }
    }

    override fun getSuppressActions(element: PsiElement?, toolId: String): Array<SuppressQuickFix> {
        // TODO: suppress action to suppress for the entire file
        return SuppressQuickFix.EMPTY_ARRAY
    }
}