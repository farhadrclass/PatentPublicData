package gov.uspto.common.io;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Buffered File Writer which automatically rolls over to a new file when
 * partition threshold is reached.
 * 
 * @author Brian G. Feldman<brian.feldman@uspto.gov>
 *
 */
public class PartitionFileWriter extends Writer {

	private static final Logger LOGGER = LoggerFactory.getLogger(PartitionFileWriter.class.getName());

	private final Path outputPath;
	private final String fileName;
	private final String fileSuffix;
	private final Charset charset;
	private String header;
	private String footer;

	private Writer writer;
	private int filePart = 0;
	private PartitionPredicate predicate;

	/**
	 * @param outputPath - output directory
	 * @param fileName   - file name
	 * @param fileSuffix - file extension suffix
	 * @param predicate  - partition threshold predicate
	 * @param charset
	 * 
	 * @see PartitionPredicate
	 */
	public PartitionFileWriter(final Path outputPath, final String fileName, final String fileSuffix,
			PartitionPredicate predicate, Charset charset) {
		this.outputPath = outputPath;
		this.fileName = fileName;
		this.fileSuffix = fileSuffix;
		this.predicate = predicate;
		this.charset = charset;
	}

	/**
	 * PartitionFileWriter which uses the default partition predicate
	 * 
	 * @param outputPath
	 * @param fileName
	 * @param fileSuffix
	 * @param recordLimit
	 * @param sizeLimitMB
	 * @param charset
	 * 
	 * @see PartitionPredicateDefault
	 */
	public PartitionFileWriter(final Path outputPath, final String fileName, final String fileSuffix,
			final int recordLimit, final int sizeLimitMB, Charset charset) {
		this(outputPath, fileName, fileSuffix, new PartitionPredicateDefault(recordLimit, sizeLimitMB), charset);
	}

	/**
	 * PartitionFileWriter which uses the default partition predicate and default character set
	 * 
	 * @param outputPath
	 * @param fileName
	 * @param fileSuffix
	 * @param recordLimit
	 * @param sizeLimitMB
	 * 
	 * @see PartitionPredicateDefault
	 */
	public PartitionFileWriter(final Path outputPath, final String fileName, final String fileSuffix,
			final int recordLimit, final int sizeLimitMB) {
		this(outputPath, fileName, fileSuffix, new PartitionPredicateDefault(recordLimit, sizeLimitMB),  Charset.defaultCharset());
	}

	public void setHeader(String header) {
		this.header = header;
	}

	public void setFooter(String footer) {
		this.footer = footer;
	}

	@Override
	public void write(String str) throws IOException {
		if (writer == null || predicate.thresholdReached(str)) {
			this.close();

			Path filePath = getOutputFilePath();
			LOGGER.info("Opening Writer: {}", filePath);
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath.toFile()), charset));
			if (header != null) {
				writer.write(header);
			}
		}

		writer.write(str);
	}

	private Path getOutputFilePath() {
		StringBuilder stb = new StringBuilder();
		stb.append(fileName);
		if (predicate.hasRecordLimit() && filePart > 0) {
			stb.append("-");
			stb.append("part");
			stb.append(filePart);
		}
		stb.append(".");
		stb.append(fileSuffix);
		return Paths.get(outputPath.toString(), stb.toString());
	}

	@Override
	public void close() throws IOException {
		if (writer != null) {
			if (footer != null) {
				writer.write(footer);
			}
			filePart++;
			predicate.restCounts();
			writer.close();
			writer = null;
		}
	}

	@Override
	public void flush() throws IOException {
		if (writer != null) {
			writer.flush();
		}
	}

	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {
		if (writer != null) {
			writer.write(cbuf, off, len);
		}
	}
}