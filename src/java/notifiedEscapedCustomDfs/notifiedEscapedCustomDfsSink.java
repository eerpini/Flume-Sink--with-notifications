package notifiedEscapedCustomDfs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.flume.conf.Context;
import com.cloudera.flume.conf.FlumeConfiguration;
import com.cloudera.flume.conf.FlumeSpecException;
import com.cloudera.flume.conf.SinkFactory.SinkBuilder;
import com.cloudera.flume.core.Event;
import com.cloudera.flume.core.EventSink;
import com.cloudera.flume.handlers.text.FormatFactory;
import com.cloudera.flume.handlers.text.output.OutputFormat;
import com.cloudera.flume.handlers.text.output.RawOutputFormat;
import com.cloudera.util.Pair;
import com.cloudera.flume.handlers.hdfs.*;
import com.google.common.base.Preconditions;
import com.testing.notifications.notify;

/**
 * Writes events the a file give a hadoop uri path. If no uri is specified It
 * defaults to the set by the given configured by fs.default.name config
 * variable. The user can specify an output format for the file. If none is
 * specified the default set by flume.collector.outputformat in the
 * flumeconfiguration file is used.
 * 
 * TODO (jon) eventually, the CustomDfsSink should be replaced with just a
 * EventSink.
 * 
 * TODO (jon) this is gross, please deprecate me.
 */
public class notifiedEscapedCustomDfsSink extends EventSink.Base {
	static final Logger LOG = LoggerFactory
			.getLogger(EscapedCustomDfsSink.class);
	final String path;
	OutputFormat format;

	CustomDfsSink writer = null;

	// We keep a - potentially unbounded - set of writers around to deal with
	// different tags on events. Therefore this feature should be used with some
	// care (where the set of possible paths is small) until we do something
	// more sensible with resource management.
	final Map<String, CustomDfsSink> sfWriters = new HashMap<String, CustomDfsSink>();

	// Used to short-circuit around doing regex matches when we know there are
	// no templates to be replaced.
	boolean shouldSub = false;
	private String filename = "";
	protected String absolutePath = "";

	public notifiedEscapedCustomDfsSink(String path, String filename, OutputFormat o) {
		this.path = path;
		this.filename = filename;
		shouldSub = Event.containsTag(path) || Event.containsTag(filename);
		this.format = o;
		absolutePath = path;
		if (filename != null && filename.length() > 0) {
			if (!absolutePath.endsWith(Path.SEPARATOR)) {
				absolutePath += Path.SEPARATOR;
			}
			absolutePath += this.filename;
		}
	}

	static protected OutputFormat getDefaultOutputFormat() {
		try {
			return FormatFactory.get().getOutputFormat(
					FlumeConfiguration.get().getDefaultOutputFormat());
		} catch (FlumeSpecException e) {
			LOG.warn("format from conf file not found, using default", e);
			return new RawOutputFormat();
		}
	}

	public notifiedEscapedCustomDfsSink(String path, String filename) {
		this(path, filename, getDefaultOutputFormat());
	}

	protected CustomDfsSink openWriter(String p) throws IOException {
		LOG.info("Opening " + p);
		CustomDfsSink w = new CustomDfsSink(p, format);
		w.open();
		return w;
	}

	/**
	 * Writes the message to an HDFS file whose path is substituted with tags
	 * drawn from the supplied event
	 */
	@Override
	public void append(Event e) throws IOException, InterruptedException {
		CustomDfsSink w = writer;
		if (shouldSub) {
			String realPath = e.escapeString(absolutePath);
			w = sfWriters.get(realPath);
			if (w == null) {
				w = openWriter(realPath);
				sfWriters.put(realPath, w);
			}
		}
		w.append(e);
		super.append(e);
	}

	@Override
	public void close() throws IOException {
		if (shouldSub) {
			for (Entry<String, CustomDfsSink> e : sfWriters.entrySet()) {
				LOG.info("Closing " + e.getKey());
				e.getValue().close();
				/*
				 * notification mechanism
				 */
				LOG.info("Requesting to send notification");
				boolean result = notify
						.sendMail("<from_email_address>",
								"<to_email_address>", "Written hdfs file :"
										+ e.getKey());
				if (result) {
					LOG.info("notification sent successfully");
				}

			}
		} else {
			LOG.info("Closing " + absolutePath);
			if (writer == null) {
				LOG.warn("notifiedEscapedCustomDfsSink's Writer for '" + absolutePath
						+ "' was already closed!");
				return;
			}
			writer.close();
			/*
			 * notification mechanism
			 */
			LOG.info("Requesting to send notification");
			boolean result = notify
					.sendMail("<from_email_address>",
							"<to_email_address>", "Written hdfs file :"
									+ absolutePath);
			if (result) {
				LOG.info("notification sent successfully");
			}
			
			writer = null;
		}
	}

	@Override
	public void open() throws IOException {
		if (!shouldSub) {
			writer = openWriter(absolutePath);
		}
	}

	public static SinkBuilder builder() {
		return new SinkBuilder() {
			@Override
			public EventSink build(Context context, String... args) {
				Preconditions.checkArgument(args.length >= 1
						&& args.length <= 3,
						"usage: notifiedEscapedCustomDfs(\"[(hdfs|file|s3n|...)://namenode[:port]]/path\""
								+ "[, file [,outputformat ]])");

				String filename = "";
				if (args.length >= 2) {
					filename = args[1];
				}

				String format = FlumeConfiguration.get()
						.getDefaultOutputFormat();
				if (args.length >= 3) {
					format = args[2];
				}

				OutputFormat o;
				try {
					o = FormatFactory.get().getOutputFormat(format);
				} catch (FlumeSpecException e) {
					LOG.warn("Illegal format type " + format + ".", e);
					o = null;
				}
				Preconditions.checkArgument(o != null, "Illegal format type "
						+ format + ".");

				return new notifiedEscapedCustomDfsSink(args[0], filename, o);
			}
		};
	}
	/**
	   * This is a special function used by the SourceFactory to pull in this class
	   * as a plugin sink.
	   */
	  public static List<Pair<String, SinkBuilder>> getSinkBuilders() {
	    List<Pair<String, SinkBuilder>> builders =
	      new ArrayList<Pair<String, SinkBuilder>>();
	    builders.add(new Pair<String, SinkBuilder>("notifiedEscapedCustomDfs", builder()));
	    return builders;
	  }
}
