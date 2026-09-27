# Contributing

Thank you for the interest in MediaKtor. If you want to contribute, fork the repository and make a pull request
with the changes. 

## GitHub issues

We use [GitHub Issues](https://github.com/Hashibane/MediaKtor/issues) to track bugs, improvements, and feature requests.

- **Bug Reports:** Please provide a minimal code snippet reproducing the problem and a description of the
    **expected** vs. **actual** behavior.


- **First-time Contributors:** Look for issues labeled with `good first issue` to find entry-level tasks.

## How to contribute?

You don't have to follow those to the letter, but you should generally follow points below. They can
be applied at any point before the merge.

1. Ensure formatting according to official [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
2. Add a few unit tests your changes
3. Ensure all tests pass with `./gradlew test`
4. Add a KDoc for any new or modified public APIs
5. Rebase with master if there had been any changes

## Contributing tests

MediaKtor uses two types of test:

- **Unit tests:** should ideally be in the same module/project. For testing the preprocessor 
    you can modify/write tests in their associated packages. As an example most of `mediaktor-core` is tested in
    `mediaktor-bare`. You should use `mediaktor-testing` DSL in unit tests that involve KSP.


- **Integration tests:** should be created as separate modules in `integrationTests/` directory with name 
    `integrationTest<YourTestName>`.