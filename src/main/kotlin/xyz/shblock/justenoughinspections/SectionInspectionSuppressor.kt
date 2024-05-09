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
import kotlinx.collections.immutable.toImmutableList
import java.util.regex.Pattern


val PATTERN_BEGIN = Pattern.compile(".*(?:begin|Begin|BEGIN)\\s*:\\s*" + SuppressionUtil.COMMON_SUPPRESS_REGEXP)!!
val PATTERN_END = Pattern.compile(".*(?:end|End|END)\\s*:\\s*" + SuppressionUtilCore.SUPPRESS_INSPECTIONS_TAG_NAME)!!

class SectionInspectionSuppressor : InspectionSuppressor {
    private fun getSuppressRanges(file: PsiFile): List<Pair<TextRange, String>> {
        val ranges = mutableListOf<Pair<TextRange, String>>()
        val stack = Stack<Pair<Int, String>>()

        fun process(element: PsiElement) {
            println(element.text)
            if (element is PsiComment) {
                val text = element.text

                val mb = PATTERN_BEGIN.matcher(text)
                if (mb.matches()) {
                    stack.push(Pair(element.endOffset + 1, mb.group(1)))
                    return
                }

                val me = PATTERN_END.matcher(text)
                if (me.matches()) {
                    if (stack.empty()) return
                    val (offset, ids) = stack.pop()
                    ranges.add(Pair(TextRange(offset, element.startOffset - 1), ids))
                    return
                }
            }
        }

        var leaf: PsiElement? = null
        val lastLeaf = PsiTreeUtil.lastChild(file)
        while (leaf !== lastLeaf) {
            leaf = if (leaf == null) PsiTreeUtil.firstChild(file) else PsiTreeUtil.nextLeaf(leaf)
            process(leaf!!)
        }

        // Handle unclosed sections
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