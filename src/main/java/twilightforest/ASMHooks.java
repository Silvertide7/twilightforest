package twilightforest;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.Musics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.EntityLeaveWorldEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fmllegacy.network.PacketDistributor;
import twilightforest.entity.TFPartEntity;
import twilightforest.network.TFPacketHandler;
import twilightforest.network.UpdateTFMultipartPacket;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@SuppressWarnings({"JavadocReference", "unused", "RedundantSuppression"})
public class ASMHooks {

	public static volatile Level world;

	/**
	 * Injection Point:<br>
	 * {@link net.minecraft.client.gui.MapRenderer.MapInstance#draw(PoseStack, MultiBufferSource, boolean, int)}<br>
	 * [BEFORE FIRST ISTORE]
	 */
	public static void mapRenderContext(PoseStack stack, MultiBufferSource buffer, int light) {
		TFMagicMapData.TFMapDecoration.RenderContext.stack = stack;
		TFMagicMapData.TFMapDecoration.RenderContext.buffer = buffer;
		TFMagicMapData.TFMapDecoration.RenderContext.light = light;
	}

	/**
	 * Injection Point:<br>
	 * {@link net.minecraft.client.sounds.MusicManager#tick()}<br>
	 * [AFTER FIRST INVOKEVIRTUAL]
	 */
	public static Music music(Music music) {
		if (Minecraft.getInstance().level != null && Minecraft.getInstance().player != null && (music == Musics.CREATIVE || music == Musics.UNDER_WATER) && Minecraft.getInstance().level.dimension().location().toString().equals(TFConfig.COMMON_CONFIG.DIMENSION.twilightForestID.get()))
			return Minecraft.getInstance().level.getBiomeManager().getNoiseBiomeAtPosition(Minecraft.getInstance().player.blockPosition()).getBackgroundMusic().orElse(Musics.GAME);
		return music;
	}

	private static final WeakHashMap<Level, List<TFPartEntity<?>>> cache = new WeakHashMap<>();

	public static void registerMultipartEvents(IEventBus bus) {
		bus.addListener((Consumer<EntityJoinWorldEvent>) event -> {
			if(event.getEntity().isMultipartEntity())
			synchronized (cache) {
				cache.computeIfAbsent(event.getWorld(), (w) -> new ArrayList<>());
				cache.get(event.getWorld()).addAll(Arrays.stream(Objects.requireNonNull(event.getEntity().getParts())).
						filter(TFPartEntity.class::isInstance).map(obj -> (TFPartEntity<?>) obj).
						collect(Collectors.toList()));

			}
		});
		bus.addListener((Consumer<EntityLeaveWorldEvent>) event -> {
			if(event.getEntity().isMultipartEntity())
			synchronized (cache) {
				cache.computeIfPresent(event.getWorld(), (world, list) -> {
					list.removeAll(Arrays.stream(Objects.requireNonNull(event.getEntity().getParts())).
							filter(TFPartEntity.class::isInstance).map(TFPartEntity.class::cast).
							collect(Collectors.toList()));
					return list;
				});
			}
		});
	}

	/**
	 * Injection Point:<br>
	 * {@link net.minecraft.world.World#getEntitiesInAABBexcluding }<br>
	 * [BEFORE ARETURN]
	 */
	public static synchronized List<Entity> multipartHitbox(List<Entity> list, Level world, @Nullable Entity entityIn, AABB boundingBox, @Nullable Predicate<? super Entity> predicate) {
		synchronized (cache) {
			List<TFPartEntity<?>> parts = cache.get(world);
			if(parts != null) {
				for (TFPartEntity<?> part : parts) {
					if (part != entityIn &&

							part.getBoundingBox().intersects(boundingBox) &&

							(predicate == null || predicate.test(part)) &&

							!list.contains(part))
						list.add(part);
				}
			}
			return list;
		}
	}

	/**
	 * Injection Point:<br>
	 * {@link net.minecraft.world.TrackedEntity#sendMetadata }<br>
	 * [AFTER GETFIELD]
	 */
	public static Entity updateMultiparts(Entity entity) {
		if (entity.isMultipartEntity())
			TFPacketHandler.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), new UpdateTFMultipartPacket(entity));
		return entity;
	}

	/**
	 * Injection Point:<br>
	 * {@link net.minecraft.client.renderer.entity.EntityRendererManager#getRenderer(Entity)}  }<br>
	 * [AFTER GETFIELD]
	 */
	@Nullable
	@OnlyIn(Dist.CLIENT)
	public static EntityRenderer<?> getMultipartRenderer(@Nullable EntityRenderer<?> renderer, Entity entity, EntityRendererProvider.Context manager) {
		if(entity instanceof TFPartEntity<?>)
			return ((TFPartEntity) entity).renderer(manager);
		return renderer;
	}

	/**
	 * Injection Point:<br>
	 * {@link net.minecraft.client.renderer.WorldRenderer#updateCameraAndRender(MatrixStack, float, long, boolean, ActiveRenderInfo, GameRenderer, LightTexture, Matrix4f)} )}  }<br>
	 * [AFTER {@link net.minecraft.client.world.ClientWorld#getAllEntities}]
	 */
	public static Iterable<Entity> renderMutiparts(Iterable<Entity> iter) {
		List<Entity> list = new ArrayList<>();
		iter.forEach(entity -> {
			list.add(entity);
			if(entity.isMultipartEntity() && entity.getParts() != null) {
				for (PartEntity<?> part : entity.getParts()) {
					if(part instanceof TFPartEntity)
						list.add(part);
				}
			}
		});
		return list;
	}

}
