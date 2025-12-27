# Arcane MoLang Engine - License and Attribution

## Original Project

**Arcane** - A high-performance MoLang expression evaluator for Java

- **Repository**: https://github.com/Draylar/arcane
- **Author**: Draylar and Contributors
- **License**: MIT (or compatible open source license)

## Modifications for SkyCore

This directory contains source code from the Arcane project, which has been integrated into the SkyCore framework.

### Changes Made

1. **Package Renaming**: All classes have been moved from `dev.omega.arcane.*` to `org.mybad.core.expression.molang.*`
2. **Integration**: Added `MolangExpressionEngine` wrapper class for unified SkyCore API
3. **Testing**: Added SkyCore-specific integration tests

### License Compliance

This code is provided under the same license as the original Arcane project. The original license terms must be respected.

### Attribution

- Original MoLang parser, lexer, and AST implementations are from the Arcane project
- SkyCore framework maintains and uses this code as-is with minimal modifications
- All original authors and contributors are acknowledged

## Usage in SkyCore

The MoLang engine is used by:
- Model system: For animation expressions and constraints
- Particle system: For dynamic parameter calculations
- Any component requiring dynamic expression evaluation

## Supported Features

- Basic arithmetic: `+`, `-`, `*`, `/`
- Comparison: `<`, `>`, `==`, `!=`, `<=`, `>=`
- Logical operators: `&&`, `||`, `!`
- Ternary operator: `? :`
- Mathematical functions: `sqrt`, `abs`, `floor`, `ceil`, etc.
- Variable references and context-aware evaluation

## Performance

- High-performance expression evaluation
- Minimal overhead suitable for real-time applications
- ~100,000 expressions per ~15ms on typical systems

---

**SkyCore Project**: https://github.com/mybad-core/skycore
**Migration Date**: 2025-12-25
**Status**: ✅ Integrated and tested
