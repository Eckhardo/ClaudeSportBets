# API Design Rules
- All endpoints: `/bulitipper/api/v1/<resource>`.
- The version lives **only** in the controller mapping:
  - `application.properties`: `server.servlet.context-path=/bulitipper/api` (no version here).
  - Controllers: `@RequestMapping("/v1/<resource>")`.
- Tests address controllers as `/v1/<resource>`: `@WebMvcTest` has no context path; integration tests prepend `server.servlet.context-path`.

## HTTP Methods & Status Codes
| Operation | Verb   | Success Response                  |
|-----------|--------|-----------------------------------|
| Create    | POST   | `201 Created` with body           |
| Retrieve  | GET    | `200 OK`                          |
| Update    | PUT    | `200 OK` with updated body        |
| Delete    | DELETE | `204 No Content`                  |

## Error Responses
- Validation errors → `400 Bad Request`, listing all field errors.
- Business rule violations → `422 Unprocessable Entity` (not 400).
- Duplicate resource → `409 Conflict`.
