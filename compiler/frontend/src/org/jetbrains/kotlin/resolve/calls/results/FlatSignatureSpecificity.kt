/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.calls.results

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.psi.ValueArgument
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilderImpl
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument
import org.jetbrains.kotlin.resolve.calls.model.MutableResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCallImpl
import org.jetbrains.kotlin.resolve.calls.results.FlatSignature.Companion.argumentValueType
import org.jetbrains.kotlin.resolve.calls.results.FlatSignature.Companion.extensionReceiverTypeOrEmpty
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import java.util.*

interface SpecificityComparisonCallbacks {
    fun isNonSubtypeNotLessSpecific(specific: KotlinType, general: KotlinType): Boolean
}

interface TypeSpecificityComparator {
    fun isDefinitelyLessSpecific(specific: KotlinType, general: KotlinType): Boolean

    object NONE: TypeSpecificityComparator {
        override fun isDefinitelyLessSpecific(specific: KotlinType, general: KotlinType) = false
    }
}

interface SimpleConstraintSystem {
    fun registerTypeVariables(typeParameters: Collection<TypeParameterDescriptor>): TypeSubstitutor
    fun addSubtypeConstraint(subType: UnwrappedType, superType: UnwrappedType)
    fun hasContradiction(): Boolean
}

fun <T> SimpleConstraintSystem.isSignatureNotLessSpecific(
        specific: FlatSignature<T>,
        general: FlatSignature<T>,
        callbacks: SpecificityComparisonCallbacks,
        specificityComparator: TypeSpecificityComparator
): Boolean {
    if (specific.hasExtensionReceiver != general.hasExtensionReceiver) return false
    if (specific.valueParameterTypes.size != general.valueParameterTypes.size) return false

    val typeParameters = general.typeParameters
    val typeSubstitutor = registerTypeVariables(typeParameters)

    for ((specificType, generalType) in specific.valueParameterTypes.zip(general.valueParameterTypes)) {
        if (specificType == null || generalType == null) continue

        if (specificityComparator.isDefinitelyLessSpecific(specificType, generalType)) {
            return false
        }

        if (typeParameters.isEmpty() || !TypeUtils.dependsOnTypeParameters(generalType, typeParameters)) {
            if (!KotlinTypeChecker.DEFAULT.isSubtypeOf(specificType, generalType)) {
                if (!callbacks.isNonSubtypeNotLessSpecific(specificType, generalType)) {
                    return false
                }
            }
        }
        else {
            val substitutedGeneralType = typeSubstitutor.safeSubstitute(generalType, Variance.INVARIANT)
            addSubtypeConstraint(specificType.unwrap(), substitutedGeneralType.unwrap())
        }
    }

    return !hasContradiction()
}


fun <RC : ResolvedCall<*>> RC.createFlatSignature(): FlatSignature<RC> {
    val originalDescriptor = candidateDescriptor.original
    val originalValueParameters = originalDescriptor.valueParameters

    var numDefaults = 0
    val valueArgumentToParameterType = HashMap<ValueArgument, KotlinType>()
    for ((valueParameter, resolvedValueArgument) in valueArguments.entries) {
        if (resolvedValueArgument is DefaultValueArgument) {
            numDefaults++
        }
        else {
            val originalValueParameter = originalValueParameters[valueParameter.index]
            val parameterType = originalValueParameter.argumentValueType
            for (valueArgument in resolvedValueArgument.arguments) {
                valueArgumentToParameterType[valueArgument] = parameterType
            }
        }
    }

    return FlatSignature(this,
                         originalDescriptor.typeParameters,
                         valueParameterTypes = originalDescriptor.extensionReceiverTypeOrEmpty() +
                                               call.valueArguments.map { valueArgumentToParameterType[it] },
                         hasExtensionReceiver = originalDescriptor.extensionReceiverParameter != null,
                         hasVarargs = originalDescriptor.valueParameters.any { it.varargElementType != null },
                         numDefaults = numDefaults)
}

fun createOverloadingConflictResolver(
        builtIns: KotlinBuiltIns,
        specificityComparator: TypeSpecificityComparator
) = OverloadingConflictResolver(
        builtIns,
        specificityComparator,
        MutableResolvedCall<*>::getResultingDescriptor,
        ConstraintSystemBuilderImpl.Companion::forSpecificity,
        MutableResolvedCall<*>::createFlatSignature,
        { (it as? VariableAsFunctionResolvedCallImpl)?.variableCall },
        { DescriptorToSourceUtils.descriptorToDeclaration(it) != null}
)