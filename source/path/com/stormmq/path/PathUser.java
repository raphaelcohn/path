package com.stormmq.path;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

@SuppressWarnings("InterfaceNeverImplemented")
@FunctionalInterface
public interface PathUser
{
	void use(@NotNull final Path path);
}
