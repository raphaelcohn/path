package com.stormmq.path;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.zip.*;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.String.format;
import static java.nio.file.FileVisitOption.FOLLOW_LINKS;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.Files.deleteIfExists;
import static java.nio.file.Files.walkFileTree;
import static java.util.Arrays.copyOf;
import static java.util.EnumSet.of;
import static java.util.Locale.ENGLISH;

public final class FileAndFolderHelper
{
	@NotNull public static final EnumSet<FileVisitOption> FollowLinks = of(FOLLOW_LINKS);
	@NotNull public static final byte[] Empty = {};

	@NotNull
	public static byte[] retrieveAllBytesForUnknownInputStreamSize(@NotNull final ZipFile zipFile, @NotNull final ZipEntry zipEntry, final int bufferSize) throws IOException, ZipException
	{
		// Most java class files are well under 1Mb and growth is x4 (<< 2)
		try (final InputStream inputStream = zipFile.getInputStream(zipEntry))
		{
			return retrieveAllBytesForUnknownInputStreamSize(inputStream, bufferSize);
		}
	}

	@NotNull
	public static byte[] retrieveAllBytesForUnknownInputStreamSize(@NotNull final InputStream inputStream, final int bufferSize) throws IOException
	{
		// Not the most efficient mechanism, as the buffer is copied on growth rather than a sequence of buffers being allocated and then all copied into one final buffer

		long totalBytesRead = 0;
		byte[] buffer = new byte[bufferSize];

		int offset = 0;
		int remainingBufferCapacity = buffer.length;
		do
		{
			final int bytesRead = inputStream.read(buffer, offset, remainingBufferCapacity);
			if (bytesRead == -1)
			{
				break;
			}
			totalBytesRead += bytesRead;
			if (totalBytesRead > Integer.MAX_VALUE)
			{
				throw new IOException("2Gb limit reached");
			}
			offset += bytesRead;
			remainingBufferCapacity -= bytesRead;
			if (remainingBufferCapacity == 0)
			{
				remainingBufferCapacity = buffer.length << 2;
				offset = 0;
				buffer = copyOf(buffer, remainingBufferCapacity);
			}
		}
		while (true);

		if (totalBytesRead == 0)
		{
			return Empty;
		}

		if (totalBytesRead == buffer.length)
		{
			return buffer;
		}

		//noinspection NumericCastThatLosesPrecision
		return copyOf(buffer, (int) totalBytesRead);
	}

	@NotNull
	public static byte[] retrieveAllBytesForKnownInputStreamSize(@NotNull final ZipFile zipFile, @NotNull final ZipEntry zipEntry, final int length) throws IOException
	{
		if (length == 0)
		{
			return Empty;
		}
		try (final InputStream inputStream = zipFile.getInputStream(zipEntry))
		{
			return retrieveAllBytesForKnownInputStreamSize(inputStream, length);
		}
	}

	@NotNull
	public static byte[] retrieveAllBytesForKnownInputStreamSize(@NotNull final InputStream inputStream, final int length) throws IOException
	{
		if (length == 0)
		{
			return Empty;
		}
		final byte[] all = new byte[length];
		int offset = 0;
		int remaining = length;
		do
		{
			final int bytesRead = inputStream.read(all, offset, remaining);
			if (bytesRead == -1)
			{
				break;
			}
			offset += bytesRead;
			remaining -= bytesRead;

		}
		while (remaining != 0);
		return all;
	}

	private FileAndFolderHelper()
	{
	}

	public static void walkTreeFollowingSymlinks(@NotNull final Path rootPath, @NotNull final FileVisitor<Path> fileVisitor)
	{
		try
		{
			walkFileTree(rootPath, FollowLinks, MAX_VALUE, new FileVisitor<Path>()
			{
				@Override
				public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException
				{
					return fileVisitor.preVisitDirectory(dir, attrs);
				}

				@Override
				public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException
				{
					return fileVisitor.visitFile(file, attrs);
				}

				@Override
				public FileVisitResult visitFileFailed(final Path file, final IOException exc) throws IOException
				{
					if (exc instanceof FileSystemLoopException)
					{
						return CONTINUE;
					}
					return fileVisitor.visitFileFailed(file, exc);
				}

				@Override
				public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException
				{
					return fileVisitor.postVisitDirectory(dir, exc);
				}
			});
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
				public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs)
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
				public FileVisitResult visitFileFailed(final Path file, final IOException exc)
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
