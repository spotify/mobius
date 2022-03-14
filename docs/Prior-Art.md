Many readers may feel like Mobius is "just like X" - and they are correct. Here's a collection of
related concepts, tools and frameworks that we're aware of and some of which we have used when
coming up with Mobius. Being aware of our biases, we don't want to try to make comparisons, but in
general, the main difference between Mobius and many of these alternatives is usually one of:

1. Mobius Update functions return Effects as immutable descriptions of things that should happen
   rather than doing them directly or returning something that knows how to execute them.
1. Mobius Update functions decide atomically which Effects and state changes should happen for a
   given Event. Contrasting this with the common approach in redux-like frameworks, there is no
   ordering between (in, say, [RxRedux](https://github.com/freeletics/RxRedux) when a SideEffect
   processes an Action (potentially leading to new Actions) and when the Reducer will do it. The
   Mobius approach makes it easier to handle races.
1. Mobius targets development in JVM/Android.
1. Mobius is a framework that encodes its ideas in a way that is less subject to interpretation than
   a more loosely defined pattern is.

Here's an unordered and incomplete list of related technologies, documents, talks and ideas that we
are aware of:

- [The Elm Architecture](https://guide.elm-lang.org/architecture/)
- [Haskell's State Monad](https://wiki.haskell.org/State_Monad)
- [Andy Matuschak's state machines](https://gist.github.com/andymatuschak/d5f0a8730ad601bcccae97e8398e25b2)
- [Rx Workflows](https://www.youtube.com/watch?v=KjoMnsc2lPo) and
  also [open-sourced](https://github.com/square/workflow)
- [Unidirectional UI architectures](https://staltz.com/unidirectional-user-interface-architectures.html)
- [Cycle.js](https://cycle.js.org/)
- [Redux](https://redux.js.org/)
- [Managing State with RxJava](https://www.youtube.com/watch?v=0IKHxjkgop4)
- [MVI](http://hannesdorfmann.com/android/mosby3-mvi-1)
- [RxRedux](https://github.com/freeletics/RxRedux)
- [Grox](https://github.com/groupon/grox)
- [Presenter First](https://en.wikipedia.org/wiki/Presenter_first_(software_approach)) - similar
  to [the Mobius Workflow](./The-Mobius-Workflow.md).
