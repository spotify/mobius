The term [pure function](https://en.wikipedia.org/wiki/Pure_function), means that the function's return value is completely determined by its inputs and that executing the function produces no side effects. A function that is not pure is called ‘impure’. This distinction is important for primarily the following reasons:

- Pure functions are fundamentally simple: the fact that they are perfectly predictable makes them easy to understand, and makes mistakes easy to correct. This in turn makes them into great building blocks when constructing complex business logic.
- Pure functions are extremely easy to test, and it's even feasible to do so exhaustively in the sense of 'covering all input values that lead to different branches'.
- In contrast, impure functions - which is what you get with object-oriented programming where an object encapsulates mutable state and behaviour - are harder to understand and test. Generally, you will need a debugger to understand the current state of the object, and to see which sequence of events led up to an unexpected state that triggers a bug.

In Mobius, we encourage using pure functions where feasible, and impure functions only where necessary.

For some more thoughts on the benefits of pure functions, see for instance http://blog.agiledeveloper.com/2015/12/benefits-of-pure-functions.html.