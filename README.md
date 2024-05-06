# JustEnoughInspections: flexible inspection suppression
An IntelliJ IDE plugin that provides more flexible inspection suppression features.

Suppression sections
---
- Suppresses all inspections of types in a section of code
- Supports nesting sections
```python
# begin: noinspection PyUnresolvedReferences

print(b) # suppressed

def test():
    print(a) # suppressed
    print(nope="hello") # not suppressed

# begin: noinspection ALL
print(nope="hello") # suppressed
# end: noinspection

# end: noinspection
```
- Details:
  - `end: noinspection`s can be omitted, if so, the section ends at the end of file. This makes it easy to suppress inspections for the whole file.
  - The prefixes are case-insensitive on the word level, there can also be extra whitespaces around the colon. Here's a few examples of possible end comments:
    - `End: noinspection`
    - `END :noinspection`
    - `End : noinspection`

Language Support
---
- Python
- Cython
- Kotlin
- Java

Known Issues
---
Sometimes inspections won't automatically update when inspection comments are changed.  
Try reopening files or editing the code to force an refresh. 
