/*
 * Copyright (C) 2017 The Dagger Authors.
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

package dagger.internal.codegen.binding;

import static com.google.common.collect.Iterables.getOnlyElement;
import static dagger.internal.codegen.extension.DaggerStreams.toImmutableList;
import static dagger.internal.codegen.xprocessing.XElements.getSimpleName;
import static dagger.internal.codegen.xprocessing.XElements.toStableString;
import static dagger.internal.codegen.xprocessing.XProcessingEnvs.getUnboundedWildcardType;
import static dagger.internal.codegen.xprocessing.XTypes.isAssignableTo;
import static dagger.internal.codegen.xprocessing.XTypes.rewrapType;

import androidx.room3.compiler.codegen.XTypeName;
import androidx.room3.compiler.processing.XMethodElement;
import androidx.room3.compiler.processing.XProcessingEnv;
import androidx.room3.compiler.processing.XType;
import androidx.room3.compiler.processing.XTypeElement;
import com.google.common.collect.ImmutableList;
import dagger.internal.codegen.base.ContributionType;
import dagger.internal.codegen.xprocessing.XExpressionType;
import dagger.internal.codegen.xprocessing.XTypeElements;
import javax.inject.Inject;

/**
 * Checks the assignability of one type to another, given a {@link ContributionType} context. This
 * is used by {@link dagger.internal.codegen.validation.BindsMethodValidator} to validate that the
 * right-hand- side of a {@link dagger.Binds} method is valid, as well as in {@link
 * dagger.internal.codegen.writing.DelegateRequestRepresentation} when the right-hand-side in
 * generated code might be an erased type due to accessibility.
 */
public final class BindsTypeChecker {
  private final XProcessingEnv processingEnv;

  @Inject
  BindsTypeChecker(XProcessingEnv processingEnv) {
    this.processingEnv = processingEnv;
  }

  /**
   * Checks the assignability of {@code rightHandSide} to {@code leftHandSide} given a {@link
   * ContributionType} context.
   */
  public boolean isAssignable(
      XExpressionType rightHandSide, XType leftHandSide, ContributionType contributionType) {
    return rightHandSide.isAssignableTo(desiredAssignableType(leftHandSide, contributionType));
  }

  /**
   * Checks the assignability of {@code rightHandSide} to {@code leftHandSide} given a {@link
   * ContributionType} context.
   */
  public boolean isAssignable(
      XType rightHandSide, XType leftHandSide, ContributionType contributionType) {
    return isAssignableTo(rightHandSide, desiredAssignableType(leftHandSide, contributionType));
  }

  private XType desiredAssignableType(XType leftHandSide, ContributionType contributionType) {
    switch (contributionType) {
      case UNIQUE:
        return leftHandSide;
      case SET:
        XType parameterizedSetType = processingEnv.getDeclaredType(setElement(), leftHandSide);
        return methodParameterType(parameterizedSetType, "add");
      case SET_VALUES:
        // TODO(b/211774331): The left hand side type should be limited to Set types.
        // NOTE: We rewrap the LHS to use java.util.Set before looking for the addAll() method
        // because Kotlin source may be using kotlin.collection.Set which does not include addAll().
        return methodParameterType(rewrapType(leftHandSide,  XTypeName.MUTABLE_SET), "addAll");
      case MAP:
        XType parameterizedMapType =
            processingEnv.getDeclaredType(mapElement(), unboundedWildcard(), leftHandSide);
        return methodParameterTypes(parameterizedMapType, "put").get(1);
    }
    throw new AssertionError("Unknown contribution type: " + contributionType);
  }

  private ImmutableList<XType> methodParameterTypes(XType type, String methodName) {
    ImmutableList<XMethodElement> methods =
        XTypeElements.getAllMethods(type.getTypeElement()).stream()
            .filter(method -> methodName.contentEquals(getSimpleName(method)))
            .collect(toImmutableList());
    if (methods.size() != 1) {
      // TODO(bcorso): This check can be removed (and rely on Iterables.getOnlyElement() below) once
      // https://github.com/google/dagger/issues/3450#issuecomment-3108716712 is fixed. For now, we
      // use a more verbose, custom error message with more information to make it easier to debug.
      throw new IllegalStateException(
          "Expected exactly one factory method for " + toStableString(type.getTypeElement())
              + " but found: "
              + methods.stream()
                  .map(
                      method ->
                          toStableString(method.getEnclosingElement())
                              + "#"
                              + toStableString(method))
                  .collect(toImmutableList()));
    }
    return ImmutableList.copyOf(getOnlyElement(methods).asMemberOf(type).getParameterTypes());
  }

  private XType methodParameterType(XType type, String methodName) {
    return getOnlyElement(methodParameterTypes(type, methodName));
  }

  private XTypeElement setElement() {
    return processingEnv.requireTypeElement(XTypeName.MUTABLE_SET);
  }

  private XTypeElement mapElement() {
    return processingEnv.requireTypeElement(XTypeName.MUTABLE_MAP);
  }

  private XType unboundedWildcard() {
    return getUnboundedWildcardType(processingEnv);
  }
}
