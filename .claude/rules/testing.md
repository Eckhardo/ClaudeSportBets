# Testing Rules
- Test class naming: `[ClassName]Test.java`.
- Use `@ExtendWith(MockitoExtension.class)` for unit tests.
- Use `@WebMvcTest` for controller tests; mock all dependencies with `@MockitoBean` (`@MockBean` was removed in Spring Boot 4).
- `@Autowired` field injection is permitted in test classes (e.g. for `MockMvcTester`).
- Follow the AAA pattern, commenting each section:
  ```
  // Arrange
  // Act
  // Assert
  ```
- Test names: `should_[expected]_when_[condition]`.
- One assertion block per test.
- Never test private methods.
