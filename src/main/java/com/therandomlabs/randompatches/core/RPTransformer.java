package com.therandomlabs.randompatches.core;

import java.util.HashMap;
import java.util.Map;
import com.therandomlabs.randompatches.RandomPatches;
import com.therandomlabs.randompatches.config.RPStaticConfig;
import com.therandomlabs.randompatches.patch.NetHandlerPlayServerPatch;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

public class RPTransformer implements IClassTransformer {
	private static final Map<String, Patch> PATCHES = new HashMap<>();

	static {
		RPStaticConfig.reload();
		RandomPatches.registerPatches();
	}

	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) {
		final Patch patch = PATCHES.get(transformedName);

		if(patch == null) {
			return basicClass;
		}

		RandomPatches.LOGGER.debug("Transforming class: " + transformedName);

		final ClassReader reader = new ClassReader(basicClass);
		final ClassNode node = new ClassNode();
		reader.accept(node, 0);

		try {
			if(!patch.apply(node)) {
				return basicClass;
			}

			final int flags;

			if(RandomPatches.SPONGEFORGE_INSTALLED &&
					patch.getClass() == NetHandlerPlayServerPatch.class) {
				flags = ClassWriter.COMPUTE_MAXS;
			} else {
				flags = ClassWriter.COMPUTE_FRAMES;
			}

			final ClassWriter writer = new RPClassWriter(flags);
			node.accept(writer);
			return writer.toByteArray();
		} catch(Exception ex) {
			RandomPatches.LOGGER.error("Failed to transform class: " + transformedName, ex);
		}

		return basicClass;
	}

	public static void register(String className, Patch patch) {
		PATCHES.put(className, patch);
	}
}