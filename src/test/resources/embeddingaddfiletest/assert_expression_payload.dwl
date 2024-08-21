%dw 2.0
import * from dw::test::Asserts
---
payload.status must contain("updated")