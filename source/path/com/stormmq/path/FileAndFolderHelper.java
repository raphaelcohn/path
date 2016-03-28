package com.stormmq.path;

import org.jetbrains.annotations.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.zip.*;

import static com.stormmq.string.Formatting.format;
import static java.lang.Integer.MAX_VALUE;
import static java.nio.file.FileVisitOption.FOLLOW_LINKS;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.Files.deleteIfExists;
import static java.nio.file.Files.walkFileTree;
import static java.util.Arrays.copyOf;
import static java.util.EnumSet.of;

public final class FileAndFolderHelper
{
	@NotNull public static final Set<FileVisitOption> FollowLinks = of(FOLLOW_LINKS);
	@NotNull private static final byte[] Empty = {};

	@NotNull
	public static byte[] retrieveAllBytesForUnknownInputStreamSize(@NotNull final ZipFile zipFile, @NotNull final ZipEntry zipEntry, final int bufferSize) throws IOException
	{
		// Most java class files are well under 1Mb and growth is x4 (<< 2)
		try (final InputStream inputStream = zipFile.getInputStream(zipEntry))
		{
			return retrieveAllBytesForUnknownInputStreamSize(inputStream, bufferSize);
		}
	}

	@NotNull
	private static byte[] retrieveAllBytesForUnknownInputStreamSize(@NotNull final InputStream inputStream, final int bufferSize) throws IOException
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
			if (totalBytesRead > MAX_VALUE)
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
	private static byte[] retrieveAllBytesForKnownInputStreamSize(@NotNull final InputStream inputStream, final int length) throws IOException
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
			walkFileTree(path, new DeletingFileVisitor());
		}
		catch (final IOException e)
		{
			throw new IllegalStateException(format("Could not remove all folders and files below '%1$s' because of '%2$s'", path.toString(), e.getMessage()), e);
		}
	}

	private static final class DeletingFileVisitor implements FileVisitor<Path>
	{
		@Override
		public FileVisitResult preVisitDirectory(@NotNull final Path dir, @NotNull final BasicFileAttributes basicFileAttributes)
		{
			return CONTINUE;
		}

		@Override
		public FileVisitResult visitFile(@NotNull final Path file, @NotNull final BasicFileAttributes basicFileAttributes) throws IOException
		{
			deleteIfExists(file);
			return CONTINUE;
		}

		@Override
		public FileVisitResult visitFileFailed(@NotNull final Path file, @NotNull final IOException exception)
		{
			if (exception instanceof FileSystemLoopException)
			{
				return CONTINUE;
			}
			throw new IllegalStateException(format("Could not visit file '%1$s' because of '%2$s'", file.toString(), exception.getMessage()), exception);
		}

		@Override
		public FileVisitResult postVisitDirectory(@Nullable final Path directory, @Nullable final IOException exception) throws IOException
		{
			if (directory != null)
			{
				deleteIfExists(directory);
			}
			return CONTINUE;
		}
	}
}
