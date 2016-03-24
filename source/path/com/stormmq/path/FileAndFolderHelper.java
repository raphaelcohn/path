package com.stormmq.path;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.zip.ZipFile;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.String.format;
import static java.nio.file.FileVisitOption.FOLLOW_LINKS;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.Files.deleteIfExists;
import static java.nio.file.Files.walkFileTree;
import static java.util.EnumSet.of;
import static java.util.Locale.ENGLISH;

public final class FileAndFolderHelper
{
	@NotNull public static final EnumSet<FileVisitOption> FollowLinks = of(FOLLOW_LINKS);

	private FileAndFolderHelper()
	{
	}

	public static void walkTreeFollowingSymlinks(@NotNull final Path rootPath, @NotNull final SimpleFileVisitor<Path> simpleFileVisitor)
	{
		try
		{
			walkFileTree(rootPath, FollowLinks, MAX_VALUE, simpleFileVisitor);
		}
		catch (final IOException e)
		{
			throw new IllegalStateException("Could not walk tree", e);
		}
	}

	// Only works if is an exact subset
	@NotNull
	public static Path relativeToRootPath(@NotNull final Path rootPath, @NotNull final Path filePathDescendingFromRootPath)
	{
		final int size = rootPath.getNameCount();
		return filePathDescendingFromRootPath.subpath(size, filePathDescendingFromRootPath.getNameCount());
	}

	public static boolean hasFileExtension(@NotNull final Path path, @NonNls @NotNull final String fileExtension)
	{
		final String fileName = path.getFileName().toString();
		return hasFileExtension(fileName, fileExtension);
	}

	public static boolean hasFileExtension(@NotNull final String fileName, @NotNull final String fileExtension)
	{
		return fileName.endsWith('.' + fileExtension);
	}

	public static void removeAllFoldersAndFilesBelowPath(@NotNull final Path path)
	{
		try
		{
			walkFileTree(path, new FileVisitor<Path>()
			{
				@Override
				public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException
				{
					return CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException
				{
					deleteIfExists(file);
					return CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(final Path file, final IOException exc) throws IOException
				{
					throw new IllegalStateException(format(ENGLISH, "Could not visit file '%1$s' because of '%2$s'", file.toString(), exc.getMessage()), exc);
				}

				@Override
				public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException
				{
					deleteIfExists(dir);
					return CONTINUE;
				}
			});
		}
		catch (final IOException e)
		{
			throw new IllegalStateException(format(ENGLISH, "Could not remove all folders and files below '%1$s' because of '%2$s'", path.toString(), e.getMessage()), e);
		}
	}

	@NotNull
	public static String getFilePathName(@NotNull final ZipFile zipFile, @NotNull final String name)
	{
		return zipFile.getName() + "!/" + name;
	}
}
