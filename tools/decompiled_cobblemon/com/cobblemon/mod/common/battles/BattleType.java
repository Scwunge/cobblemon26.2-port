/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 *  kotlin.Metadata
 *  kotlin.jvm.internal.Intrinsics
 *  net.minecraft.network.RegistryFriendlyByteBuf
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.ComponentSerialization
 *  net.minecraft.network.chat.MutableComponent
 *  org.jetbrains.annotations.NotNull
 */
package com.cobblemon.mod.common.battles;

import com.cobblemon.mod.common.battles.BattleTypes;
import com.cobblemon.mod.common.net.IntSize;
import com.cobblemon.mod.common.util.BufferUtilsKt;
import com.cobblemon.mod.common.util.NetExtensionsKt;
import io.netty.buffer.ByteBuf;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={2, 2, 0}, k=1, xi=48, d1={"\u0000(\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\b\n\u0002\b\t\bf\u0018\u0000 \u00162\u00020\u0001:\u0001\u0016J\u0017\u0010\u0004\u001a\u00020\u00022\u0006\u0010\u0003\u001a\u00020\u0002H\u0016\u00a2\u0006\u0004\b\u0004\u0010\u0005R\u0014\u0010\t\u001a\u00020\u00068&X\u00a6\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0007\u0010\bR\u0014\u0010\r\u001a\u00020\n8&X\u00a6\u0004\u00a2\u0006\u0006\u001a\u0004\b\u000b\u0010\fR\u0014\u0010\u0011\u001a\u00020\u000e8&X\u00a6\u0004\u00a2\u0006\u0006\u001a\u0004\b\u000f\u0010\u0010R\u0014\u0010\u0013\u001a\u00020\u000e8&X\u00a6\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0012\u0010\u0010R\u0014\u0010\u0015\u001a\u00020\u000e8VX\u0096\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0014\u0010\u0010\u00a8\u0006\u0017\u00c0\u0006\u0003"}, d2={"Lcom/cobblemon/mod/common/battles/BattleType;", "", "Lnet/minecraft/network/RegistryFriendlyByteBuf;", "buffer", "saveToBuffer", "(Lnet/minecraft/network/RegistryFriendlyByteBuf;)Lnet/minecraft/network/RegistryFriendlyByteBuf;", "", "getName", "()Ljava/lang/String;", "name", "Lnet/minecraft/network/chat/MutableComponent;", "getDisplayName", "()Lnet/minecraft/network/chat/MutableComponent;", "displayName", "", "getActorsPerSide", "()I", "actorsPerSide", "getSlotsPerActor", "slotsPerActor", "getPokemonPerSide", "pokemonPerSide", "Companion", "common"})
public interface BattleType {
    @NotNull
    public static final Companion Companion = com.cobblemon.mod.common.battles.BattleType$Companion.$$INSTANCE;

    @NotNull
    public String getName();

    @NotNull
    public MutableComponent getDisplayName();

    public int getActorsPerSide();

    public int getSlotsPerActor();

    default public int getPokemonPerSide() {
        return this.getActorsPerSide() * this.getSlotsPerActor();
    }

    @NotNull
    default public RegistryFriendlyByteBuf saveToBuffer(@NotNull RegistryFriendlyByteBuf buffer) {
        Intrinsics.checkNotNullParameter((Object)buffer, (String)"buffer");
        BufferUtilsKt.writeString((ByteBuf)buffer, this.getName());
        ComponentSerialization.STREAM_CODEC.encode((Object)buffer, (Object)this.getDisplayName());
        NetExtensionsKt.writeSizedInt((ByteBuf)buffer, IntSize.U_BYTE, this.getActorsPerSide());
        NetExtensionsKt.writeSizedInt((ByteBuf)buffer, IntSize.U_BYTE, this.getSlotsPerActor());
        return buffer;
    }

    @Metadata(mv={2, 2, 0}, k=1, xi=48, d1={"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u0086\u0003\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003J\u0015\u0010\u0007\u001a\u00020\u00062\u0006\u0010\u0005\u001a\u00020\u0004\u00a2\u0006\u0004\b\u0007\u0010\b\u00a8\u0006\t"}, d2={"Lcom/cobblemon/mod/common/battles/BattleType$Companion;", "", "<init>", "()V", "Lnet/minecraft/network/RegistryFriendlyByteBuf;", "buffer", "Lcom/cobblemon/mod/common/battles/BattleType;", "loadFromBuffer", "(Lnet/minecraft/network/RegistryFriendlyByteBuf;)Lcom/cobblemon/mod/common/battles/BattleType;", "common"})
    public static final class Companion {
        static final /* synthetic */ Companion $$INSTANCE;

        private Companion() {
        }

        @NotNull
        public final BattleType loadFromBuffer(@NotNull RegistryFriendlyByteBuf buffer) {
            Intrinsics.checkNotNullParameter((Object)buffer, (String)"buffer");
            String name = BufferUtilsKt.readString((ByteBuf)buffer);
            Component displayName = (Component)ComponentSerialization.STREAM_CODEC.decode((Object)buffer);
            int actorsPerSide = NetExtensionsKt.readSizedInt((ByteBuf)buffer, IntSize.U_BYTE);
            int slotsPerActor = NetExtensionsKt.readSizedInt((ByteBuf)buffer, IntSize.U_BYTE);
            MutableComponent mutableComponent = displayName.copy();
            Intrinsics.checkNotNullExpressionValue((Object)mutableComponent, (String)"copy(...)");
            return BattleTypes.INSTANCE.makeBattleType(name, mutableComponent, actorsPerSide, slotsPerActor);
        }

        static {
            $$INSTANCE = new Companion();
        }
    }

    @Metadata(mv={2, 2, 0}, k=3, xi=48)
    public static final class DefaultImpls {
        @Deprecated
        public static int getPokemonPerSide(@NotNull BattleType $this) {
            return $this.getPokemonPerSide();
        }

        @Deprecated
        @NotNull
        public static RegistryFriendlyByteBuf saveToBuffer(@NotNull BattleType $this, @NotNull RegistryFriendlyByteBuf buffer) {
            Intrinsics.checkNotNullParameter((Object)buffer, (String)"buffer");
            return $this.saveToBuffer(buffer);
        }
    }
}
