package com.stormmq.path;

import com.stormmq.string.Api;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

@FunctionalInterface
public interface PathUser
{
	void use(@NotNull final Path path);
}
