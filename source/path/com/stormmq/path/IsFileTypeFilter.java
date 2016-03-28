package com.stormmq.path;

import com.stormmq.java.parsing.utilities.ReservedIdentifiers;
import com.stormmq.string.Api;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

import static com.stormmq.path.FileAndFolderHelper.hasFileExtension;
import static java.nio.file.Files.isReadable;
import static java.nio.file.Files.isRegularFile;

public final class IsFileTypeFilter extends AbstractDirectoryFilter
{
	@NotNull @NonNls private static final String java = "java";
	@NotNull @NonNls private static final String _class = ReservedIdentifiers._class;
	@Api @NotNull public static final IsFileTypeFilter IsJavaFile = new IsFileTypeFilter(java);
	@NotNull public static final IsFileTypeFilter IsClassFile = new IsFileTypeFilter(_class);
	@Api @NotNull public static final IsFileTypeFilter IsJavaOrClassFile = new IsFileTypeFilter(java, _class);
	@NotNull public static final IsFileTypeFilter IsJarOrZipFile = new IsFileTypeFilter("jar", "zip");

	public static boolean isJavaFile(@NonNls @NotNull final String name)
	{
		return hasFileExtension(name, java);
	}

	public static boolean isClassFile(@NonNls @NotNull final String name)
	{
		return hasFileExtension(name, _class);
	}

	@NotNull private final String[] fileExtensions;

	private IsFileTypeFilter(@NonNls @NotNull final String... fileExtensions)
	{
		this.fileExtensions = fileExtensions;
	}

	@Override
	public boolean accept(@NotNull final Path entry)
	{
		for (final String fileExtension : fileExtensions)
		{
			if (hasFileExtension(entry, fileExtension))
			{
				return isReadable(entry) && isRegularFile(entry);
			}
		}
		return false;
	}
}
