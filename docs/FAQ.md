# How do I re-render only the part of the UI that changed when I get a new model?

We believe Mobius is and will be a better framework if it’s focused on a single job. That job is
managing changes in state over time, not how to present that state to the user. It is
straightforward to build a layer between Mobius and the view layer that figures out what has changed
in the model and what hasn’t, forwarding only changes to the View. But such a layer should be
considered an add-on on top of Mobius, and will never be part of the core Mobius library. In a
typical presenter there are already cases where you get external data and have to “diff” it against
the old data, and doing that diffing “manually” is often reasonably straight forward. The only
difference when using Mobius is that you get the data from the MobiusLoop rather than directly from
the data source.

# Is Mobius an alternative to MV*?

Yes and no, but mostly no. Mobius isn’t a direct replacement or an alternative to MV* (meaning MVC,
MVP, MVVM, etc.), but it works very well as a building block in the various MV* patterns, and it
concerns itself with primarily the M part of the problem that the MV* patterns approach.

Mobius defines a way to organise your business logic (the things that usually go into the ‘M’ in MV*
, think “interactors” and “use cases” from Clean Architecture), and gives you a unidirectional
interface between business logic and the view (in the form of events and models). This means it can
be used as a replacement for the ‘M’ in for instance MVP or MVVM, keeping the relationship between
Presenter/ViewModel and the ‘V’ unchanged.

Since Mobius will give you a model with all the data you need in one place, a Presenter/ViewModel
used in conjunction with a MobiusLoop usually ends up doing little more than just formatting data
for the view, (eg. converting Date objects into formatted strings.) and dispatching Events back to
the loop. If you don’t want that layer of indirection, then Mobius can be thought of as an
alternative to MV* - you can use just a MobiusLoop and a View, with no need for a third thing that
connects them.
