package aQute.libg.glob;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * A reusable path set using Ant-style include and exclude globs.
 */
public class PathSet {
	private final List<Pattern>	includes	= new ArrayList<>();
	private final List<Pattern>	excludes	= new ArrayList<>();

	/**
	 * Create a path set.
	 */
	public PathSet() {}

	/**
	 * Create a path set with initial Ant-style globs for the include patterns.
	 *
	 * @param includes Add Ant-style globs.
	 */
	public PathSet(String... includes) {
		include(includes);
	}

	/**
	 * Add Ant-style globs to the include patterns.
	 *
	 * @param includes Add Ant-style globs.
	 * @return This PathSet.
	 */
	public PathSet includes(List<String> includes) {
		if (includes != null) {
			addPatterns(includes.stream(), this.includes);
		}
		return this;
	}

	/**
	 * Add Ant-style globs to the include patterns.
	 *
	 * @param includes Add Ant-style globs.
	 * @return This PathSet.
	 */
	public PathSet include(String... includes) {
		if (includes != null) {
			addPatterns(Arrays.stream(includes), this.includes);
		}
		return this;
	}

	/**
	 * Add Ant-style globs to the exclude patterns.
	 *
	 * @param excludes Add Ant-style globs.
	 * @return This PathSet.
	 */
	public PathSet exclude(String... excludes) {
		if (excludes != null) {
			addPatterns(Arrays.stream(excludes), this.excludes);
		}
		return this;
	}

	/**
	 * Add Ant-style globs to the exclude patterns.
	 *
	 * @param excludes Add Ant-style globs.
	 * @return This PathSet.
	 */
	public PathSet excludes(List<String> excludes) {
		if (excludes != null) {
			addPatterns(excludes.stream(), this.excludes);
		}
		return this;
	}

	private static List<Pattern> addPatterns(Stream<String> globs, List<Pattern> patterns) {
		globs.filter(Objects::nonNull)
			.map(AntGlob::toPattern)
			.forEachOrdered(patterns::add);
		return patterns;
	}

	/**
	 * Return a list of paths in the specified collection matching the
	 * configured include and exclude Ant-style glob expressions.
	 *
	 * @param defaultIncludes The default include patterns to use if no include
	 *            patterns were configured.
	 * @return A list of paths in the specified collection which match the
	 *         include and exclude Ant-style globs.
	 */
	public List<String> paths(Collection<String> paths, String... defaultIncludes) {
		return paths(paths, matches(defaultIncludes));
	}

	/**
	 * Return a list of paths in the specified collection matching the
	 * configured include and exclude Ant-style glob expressions.
	 *
	 * @param defaultIncludes The default include patterns to use if no include
	 *            patterns were configured.
	 * @return A list of paths in the specified collection which match the
	 *         include and exclude Ant-style globs.
	 */
	public List<String> paths(Collection<String> paths, List<String> defaultIncludes) {
		return paths(paths, matches(defaultIncludes));
	}

	/**
	 * Return a list of paths in the specified collection matching the
	 * configured include and exclude Ant-style glob expressions.
	 *
	 * @return A list of paths in the specified collection which match the
	 *         include and exclude Ant-style globs.
	 */
	public List<String> paths(Collection<String> paths) {
		return paths(paths, matches());
	}

	private static List<String> paths(Collection<String> paths, Predicate<String> matches) {
		return paths.stream()
			.filter(matches)
			.collect(toList());
	}

	/**
	 * Return a predicate matching the configured include and exclude Ant-style
	 * glob expressions.
	 *
	 * @param defaultIncludes The default include patterns to use if no include
	 *            patterns were configured.
	 * @return A predicate which matches the include and exclude Ant-style
	 *         globs.
	 */
	public Predicate<String> matches(String... defaultIncludes) {
		return matcher(Matcher::matches, defaultIncludes);
	}

	/**
	 * Return a predicate matching the configured include and exclude Ant-style
	 * glob expressions.
	 *
	 * @param defaultIncludes The default include patterns to use if no include
	 *            patterns were configured.
	 * @return A predicate which matches the include and exclude Ant-style
	 *         globs.
	 */
	public Predicate<String> matches(List<String> defaultIncludes) {
		return matcher(Matcher::matches, defaultIncludes);
	}

	/**
	 * Return a predicate matching the configured include and exclude Ant-style
	 * glob expressions.
	 *
	 * @return A predicate which matches the include and exclude Ant-style
	 *         globs.
	 */
	public Predicate<String> matches() {
		return matcher(Matcher::matches);
	}

	/**
	 * Return a predicate finding the configured include and exclude Ant-style
	 * glob expressions.
	 *
	 * @param defaultIncludes The default include patterns to use if no include
	 *            patterns were configured.
	 * @return A predicate which finds the include and exclude Ant-style globs.
	 */
	public Predicate<String> find(String... defaultIncludes) {
		return matcher(Matcher::find, defaultIncludes);
	}

	/**
	 * Return a predicate finding the configured include and exclude Ant-style
	 * glob expressions.
	 *
	 * @param defaultIncludes The default include patterns to use if no include
	 *            patterns were configured.
	 * @return A predicate which finds the include and exclude Ant-style globs.
	 */
	public Predicate<String> find(List<String> defaultIncludes) {
		return matcher(Matcher::find, defaultIncludes);
	}

	/**
	 * Return a predicate finding the configured include and exclude Ant-style
	 * glob expressions.
	 *
	 * @return A predicate which find the include and exclude Ant-style globs.
	 */
	public Predicate<String> find() {
		return matcher(Matcher::find);
	}

	private Predicate<String> matcher(Predicate<Matcher> predicate, String... defaultIncludes) {
		if (includes.isEmpty() && (defaultIncludes != null) && (defaultIncludes.length > 0)) {
			return matcher(predicate, addPatterns(Arrays.stream(defaultIncludes), new ArrayList<>()), excludes);
		}
		return matcher(predicate, includes, excludes);
	}

	private Predicate<String> matcher(Predicate<Matcher> predicate, List<String> defaultIncludes) {
		if (includes.isEmpty() && (defaultIncludes != null) && !defaultIncludes.isEmpty()) {
			return matcher(predicate, addPatterns(defaultIncludes.stream(), new ArrayList<>()), excludes);
		}
		return matcher(predicate, includes, excludes);
	}

	private Predicate<String> matcher(Predicate<Matcher> predicate) {
		return matcher(predicate, includes, excludes);
	}

	private static Predicate<String> matcher(Predicate<Matcher> predicate, List<Pattern> includePatterns,
		List<Pattern> excludePatterns) {
		if (includePatterns.isEmpty()) {
			return path -> false;
		}
		if (excludePatterns.isEmpty()) {
			if (includePatterns.size() == 1) {
				Pattern include = includePatterns.get(0);
				return path -> predicate.test(include.matcher(path));
			}
			List<Pattern> includes = new ArrayList<>(includePatterns);
			return path -> includes.stream()
				.anyMatch(include -> predicate.test(include.matcher(path)));
		}
		List<Pattern> includes = new ArrayList<>(includePatterns);
		List<Pattern> excludes = new ArrayList<>(excludePatterns);
		return path -> includes.stream()
			.anyMatch(include -> predicate.test(include.matcher(path)))
			&& excludes.stream()
				.noneMatch(exclude -> predicate.test(exclude.matcher(path)));
	}
}
