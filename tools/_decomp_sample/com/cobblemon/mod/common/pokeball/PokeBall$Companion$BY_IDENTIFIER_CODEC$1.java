/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  kotlin.Metadata
 *  kotlin.jvm.functions.Function1
 *  kotlin.jvm.internal.FunctionReferenceImpl
 *  kotlin.jvm.internal.Intrinsics
 *  net.minecraft.resources.ResourceLocation
 */
package com.cobblemon.mod.common.pokeball;

import com.cobblemon.mod.common.api.pokeball.PokeBalls;
import com.cobblemon.mod.common.pokeball.PokeBall;
import kotlin.Metadata;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.FunctionReferenceImpl;
import kotlin.jvm.internal.Intrinsics;
import net.minecraft.resources.ResourceLocation;

@Metadata(mv={2, 2, 0}, k=3, xi=48)
static final class PokeBall.Companion.BY_IDENTIFIER_CODEC.1
extends FunctionReferenceImpl
implements Function1<ResourceLocation, PokeBall> {
    PokeBall.Companion.BY_IDENTIFIER_CODEC.1(Object receiver) {
        super(1, receiver, PokeBalls.class, "getPokeBall", "getPokeBall(Lnet/minecraft/resources/ResourceLocation;)Lcom/cobblemon/mod/common/pokeball/PokeBall;", 0);
    }

    public final PokeBall invoke(ResourceLocation p0) {
        Intrinsics.checkNotNullParameter((Object)p0, (String)"p0");
        return PokeBalls.getPokeBall(p0);
    }
}
