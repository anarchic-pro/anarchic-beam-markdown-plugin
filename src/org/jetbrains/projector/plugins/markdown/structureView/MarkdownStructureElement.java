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
package org.jetbrains.projector.plugins.markdown.structureView;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.structureView.impl.common.PsiTreeElementBase;
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.LocationPresentation;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.ui.Queryable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.PsiFileImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.projector.plugins.markdown.util.MarkdownPsiUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class MarkdownStructureElement extends PsiTreeElementBase<PsiElement> implements SortableTreeElement, LocationPresentation,
                                                                                        Queryable {

  private static final ItemPresentation DUMMY_PRESENTATION = new MarkdownBasePresentation() {

    @Nullable
    @Override
    public String getPresentableText() {
      return null;
    }

    @Nullable
    @Override
    public String getLocationString() {
      return null;
    }
  };

  MarkdownStructureElement(@NotNull PsiElement element) {
    super(element);
  }

  @Override
  public boolean canNavigate() {
    return getElement() instanceof NavigationItem && ((NavigationItem)getElement()).canNavigate();
  }

  @Override
  public boolean canNavigateToSource() {
    return getElement() instanceof NavigationItem && ((NavigationItem)getElement()).canNavigateToSource();
  }


  @Override
  public void navigate(boolean requestFocus) {
    if (getElement() instanceof NavigationItem) {
      ((NavigationItem)getElement()).navigate(requestFocus);
    }
  }

  @NotNull
  @Override
  public String getAlphaSortKey() {
    return StringUtil.notNullize(getElement() instanceof NavigationItem ?
                                 ((NavigationItem)getElement()).getName() : null);
  }

  @Override
  public boolean isSearchInLocationString() {
    return true;
  }

  @Nullable
  @Override
  public String getPresentableText() {
    final PsiElement tag = getElement();
    if (tag == null) {
      return IdeBundle.message("node.structureview.invalid");
    }
    return getPresentation().getPresentableText();
  }

  @Override
  public String getLocationString() {
    return getPresentation().getLocationString();
  }

  @NotNull
  @Override
  public ItemPresentation getPresentation() {
    if (getElement() instanceof PsiFileImpl) {
      ItemPresentation filePresent = ((PsiFileImpl)getElement()).getPresentation();
      return filePresent != null ? filePresent : DUMMY_PRESENTATION;
    }

    if (getElement() instanceof NavigationItem) {
      final ItemPresentation itemPresent = ((NavigationItem)getElement()).getPresentation();
      if (itemPresent != null) {
        return itemPresent;
      }
    }

    return DUMMY_PRESENTATION;
  }


  @NotNull
  @Override
  public Collection<StructureViewTreeElement> getChildrenBase() {
    final ArrayList<StructureViewTreeElement> elements = new ArrayList<>();
    MarkdownPsiUtil.processContainer(getElement(), element -> elements.add(new MarkdownStructureElement(element)), element -> {
    });
    return elements;
  }

  @NotNull
  @Override
  public String getLocationPrefix() {
    return " ";
  }

  @NotNull
  @Override
  public String getLocationSuffix() {
    return "";
  }

  @Override
  public void putInfo(@NotNull Map<String, String> info) {
    info.put("text", getPresentableText());
    if (!(getElement() instanceof PsiFileImpl)) {
      info.put("location", getLocationString());
    }
  }
}
