/*
 * MIT License
 *
 * Copyright (c) 2019-2020 JetBrains s.r.o.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.jetbrains.projector.plugins.markdown.folding;

import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.CustomFoldingBuilder;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.projector.plugins.markdown.MarkdownBundle;
import org.jetbrains.projector.plugins.markdown.lang.MarkdownElementTypes;
import org.jetbrains.projector.plugins.markdown.lang.MarkdownTokenTypes;
import org.jetbrains.projector.plugins.markdown.lang.psi.MarkdownElementVisitor;
import org.jetbrains.projector.plugins.markdown.lang.psi.MarkdownPsiElement;
import org.jetbrains.projector.plugins.markdown.lang.psi.MarkdownRecursiveElementVisitor;
import org.jetbrains.projector.plugins.markdown.lang.psi.impl.*;
import org.jetbrains.projector.plugins.markdown.util.MarkdownPsiUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MarkdownFoldingBuilder extends CustomFoldingBuilder implements DumbAware {
  public static final Map<IElementType, String> TYPES_PRESENTATION_MAP = new HashMap<>();

  static {
    TYPES_PRESENTATION_MAP.put(MarkdownElementTypes.ATX_1, MarkdownBundle.message("markdown.folding.atx.1.name"));
    TYPES_PRESENTATION_MAP.put(MarkdownElementTypes.ATX_2, MarkdownBundle.message("markdown.folding.atx.2.name"));
    TYPES_PRESENTATION_MAP.put(MarkdownElementTypes.ATX_3, MarkdownBundle.message("markdown.folding.atx.3.name"));
    TYPES_PRESENTATION_MAP.put(MarkdownElementTypes.ATX_4, MarkdownBundle.message("markdown.folding.atx.4.name"));
    TYPES_PRESENTATION_MAP.put(MarkdownElementTypes.ATX_5, MarkdownBundle.message("markdown.folding.atx.5.name"));
    TYPES_PRESENTATION_MAP.put(MarkdownElementTypes.ATX_6, MarkdownBundle.message("markdown.folding.atx.6.name"));
    TYPES_PRESENTATION_MAP.put(MarkdownElementTypes.ORDERED_LIST, MarkdownBundle.message("markdown.folding.ordered.list.name"));
    TYPES_PRESENTATION_MAP.put(MarkdownElementTypes.UNORDERED_LIST, MarkdownBundle.message("markdown.folding.unordered.list.name"));
    TYPES_PRESENTATION_MAP.put(MarkdownElementTypes.BLOCK_QUOTE, MarkdownBundle.message("markdown.folding.block.quote.name"));
    TYPES_PRESENTATION_MAP.put(MarkdownElementTypes.TABLE, MarkdownBundle.message("markdown.folding.table.name"));
    TYPES_PRESENTATION_MAP.put(MarkdownElementTypes.CODE_FENCE, MarkdownBundle.message("markdown.folding.code.fence.name"));
  }

  @Override
  protected void buildLanguageFoldRegions(@NotNull List<FoldingDescriptor> descriptors,
                                          @NotNull PsiElement root,
                                          @NotNull Document document,
                                          boolean quick) {
    root.accept(new MarkdownElementVisitor() {
      @Override
      public void visitElement(@NotNull PsiElement element) {
        super.visitElement(element);
        element.acceptChildren(this);
      }

      @Override
      public void visitList(@NotNull MarkdownListImpl list) {
        addDescriptors(list);
        super.visitList(list);
      }

      @Override
      public void visitParagraph(@NotNull MarkdownParagraphImpl paragraph) {
        PsiElement parent = paragraph.getParent();
        if (parent instanceof MarkdownBlockQuoteImpl && PsiTreeUtil.findChildrenOfType(parent, MarkdownParagraphImpl.class).size() <= 1) {
          return;
        }

        addDescriptors(paragraph);
        super.visitParagraph(paragraph);
      }

      @Override
      public void visitTable(@NotNull MarkdownTableImpl table) {
        addDescriptors(table);
        super.visitTable(table);
      }

      @Override
      public void visitBlockQuote(@NotNull MarkdownBlockQuoteImpl blockQuote) {
        addDescriptors(blockQuote);
        super.visitBlockQuote(blockQuote);
      }

      @Override
      public void visitCodeFence(@NotNull MarkdownCodeFenceImpl codeFence) {
        addDescriptors(codeFence);
        super.visitCodeFence(codeFence);
      }

      private void addDescriptors(@NotNull MarkdownPsiElement element) {
        MarkdownFoldingBuilder.addDescriptors(element, element.getTextRange(), descriptors, document);
      }
    });

    root.accept(new MarkdownRecursiveElementVisitor() {
      @Override
      public void visitHeader(@NotNull MarkdownHeaderImpl header) {
        MarkdownPsiUtil.processContainer(header, endHeader -> {
        }, endHeader -> {
          PsiElement lastFileChild = header.getContainingFile().getLastChild().getLastChild();

          PsiElement prevEndFolding;
          if (endHeader == null) {
            prevEndFolding =
              PsiUtilCore.getElementType(lastFileChild) == MarkdownTokenTypes.EOL ? skipNewLinesBackward(lastFileChild) : lastFileChild;
          }
          else {
            prevEndFolding = skipNewLinesBackward(endHeader);
          }

          if (prevEndFolding == null) return;

          final TextRange range =
            TextRange.create(header.getTextRange().getStartOffset(), prevEndFolding.getTextRange().getEndOffset());

          addDescriptors(header, range, descriptors, document);
        });

        super.visitHeader(header);
      }
    });
  }

  @Override
  protected String getLanguagePlaceholderText(@NotNull ASTNode node, @NotNull TextRange range) {
    IElementType elementType = PsiUtilCore.getElementType(node);
    String explicitName = TYPES_PRESENTATION_MAP.get(elementType);
    final String prefix = explicitName != null ? explicitName + ": " : "";

    return prefix + StringUtil.shortenTextWithEllipsis(node.getText(), 30, 5);
  }

  @Override
  protected boolean isRegionCollapsedByDefault(@NotNull ASTNode node) {
    return false;
  }

  public static void addDescriptors(@NotNull MarkdownPsiElement element,
                                    @NotNull TextRange range,
                                    @NotNull List<? super FoldingDescriptor> descriptors,
                                    @NotNull Document document) {
    if (document.getLineNumber(range.getStartOffset()) != document.getLineNumber(range.getEndOffset() - 1)) {
      descriptors.add(new FoldingDescriptor(element, range));
    }
  }

  @Nullable
  public static PsiElement skipNewLinesBackward(@Nullable PsiElement element) {
    if (element == null) return null;
    for (PsiElement e = element.getPrevSibling(); e != null; e = e.getPrevSibling()) {
      if (e.getNode().getElementType() != MarkdownTokenTypes.EOL) return e;
    }
    return null;
  }
}
