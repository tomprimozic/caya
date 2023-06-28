# Caya - a programming language experiment

## Goals

- a useful yet simple language
- not aiming for performance _per se_, but choosing constructs and patterns that _can be_ optimised
- interactive, optionally statically typed, functional & imperative, simple & predictable syntax & semantics

#### current priorities

1. implement enough features to stop using host language for library implementation
2. bytecode interpreter (and overall refactor)
3. performance improvements

## Implementation

We use Java / JVM as the host platform.

Java has improved a lot in recent years - records, pattern matching, `var` type inference - so it's not terrible to use any more. In addition, `bison` (parser generator) can generate Java parsers.

JVM is one of the few runtimes with a sensible memory model (no _undefined behaviour_ - the other being _OCaml_) and has great support for concurrency (threading) and memory management (garbage collection). It generates decently fast code and enables interesting optimisation strategies in the future (directly generating JVM bytecode, Truffle/Graal optimised interpreters). Finally, it makes available a plethora of third-party libraries.

## Progress

- [x] keyword arguments, default arguments
- [x] exceptions
- [x] hashing, dict
- [x] iterators
- [x] modules
- [ ] first-class types, `typeof`
- [ ] inheritance (single)
- [ ] equality, comparisons
- [ ] `has_attr`, `list_attrs`, _runtime services_ like `call_function`, `invoke_method`, `typeof`, `int_to_str`, `plus_int32`, `get_field`, `set_item`, ...
- [x] classes
- [x] block if
- [x] `while` loop, `for` loop
- [x] `break`, `continue`
- [ ] library: str, atom, dict, list, index, vector, set, json, requests, csv, plotting, regex, math, random, datetime, os functions, hashing, logging, print, urllib, serialization (pickling), HTML, xls, postgresql, sqlite, tqdm
- [x] block functions
- [x] whitespace syntax
- [x] loading modules
- [x] operators
- [ ] rational numbers
- [ ] `float`
- [x] logic operators
- [x] tuples, records
- [x] recursive functions
- [x] Java interop
- [ ] underscore `_` (implicit functions: `_.x`, `_ * 2`)
- [ ] static modules
- [x] comparison operators
- [x] immutable data structures (vector, index)
- [x] mutable data structures (list, dict)
- [ ] array (multi-dimensional, numeric, fixed size, mutable)
- [x] immutable & mutable literals
- [ ] add tests for error messages
- [ ] function overloading
- [ ] custom operators
- [ ] `let` and `var` declarations
- [ ] destructuring assignment
- [ ] pattern matching
- [ ] "parent scope" declarations (function-level or toplevel)
- [ ] spread arguments
- [ ] list comprehensions
- [x] atoms
- [ ] macros, attributes
- [ ] bytecode, bytecode interpreter
- [ ] tail calls
- [ ] threads
- [ ] type checking, type optimisations