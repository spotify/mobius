# The Mobius Workflow

While developing the Mobius framework, we often found ourselves using real problems as examples when
teaching how to use Mobius. After a while, we noticed that we always used the same structure to
model the problems and, as a consequence, everyone in the team got a better understanding of what
they were trying to solve and many edge-cases were discovered early on.

A couple of teams started using this process as a tool to explore requirements early on in the
project and use the findings to plan what to do, and their efforts have been successful. We call
this process the Mobius workflow, and it’s the recommended approach when planning and designing
Mobius loops. Furthermore, it is recommended to take this approach as a team and involve everyone
with different backgrounds.

Through this collaboration on requirements discovery and planning, everyone on team ends up with a
shared understanding and clarity on what the desired outcome is and since the output is directly
translatable into a Mobius implementation, no details are lost in translation. Moreover,
collaborative modeling enables the team to uncover edge-cases and specify how those should be dealt
with.

## Step 1: Model it (a.k.a MoFlow)

When defining your Mobius Update function, start by determining what it'll describe. Consider
Init/Update functions as a behavior specification for your program. In order to articulate this
behavior, you need to:

1. Establish a lexicon that can be used to describe your behavior accurately
1. Create the model that describes your program’s state

We’ve found that this step is usually most efficient when people from different disciplines
participate. Doing this first step in a group setting using a whiteboard helps the team figure out
all the required events for their particular domain.

As previously mentioned, Events in Mobius can be used to represent:

1. User Interactions
1. Effects Feedback
1. External Events

These event categories can be used as a guide for establishing the necessary constructs of a Mobius
program.

### Define external events

Start by identifying external events that provide you with necessary information about the
environment your program is running in. For example, it must have internet connectivity to be able
to load data from backend. Therefore, you can start by adding an event that notifies your update
function of internet connectivity changes.

<table>
    <thead>
        <tr>
            <th align="center">Events</th>
            <th align="center">Model</th>
            <th align="center">Effects</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td align="center"><b><i>InternetStateChanged</i></b></td>
            <td rowspan=4></td>
            <td></td>
        </tr>
    </tbody>
</table>

### Capture user interactions

Next you should define user interaction events. Think button taps, navigation, and user input. Take
a close look at the designs you have and add an event for every interaction that a user can have
with your app. For example, in our login loop a user can input their email and password, they can
also tap the Login button and they can choose to navigate to the restore password screen.

<table>
    <thead>
        <tr>
            <th align="center">Events</th>
            <th align="center">Model</th>
            <th align="center">Effects</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td align="center">InternetStateChanged</td>
            <td rowspan=5></td>
            <td></td>
        </tr>
        <tr>
            <td align="center"><b><i>EmailInputChanged</i></b></td>
            <td></td>
        </tr>
        <tr>
            <td align="center"><b><i>PasswordInputChanged</i></b></td>
            <td></td>
        </tr>
        <tr>
            <td align="center"><b><i>LoginButtonClicked</i></b></td>
            <td></td>
        </tr>
        <tr>
            <td align="center"><b><i>ForgotPasswordClicked</i></b></td>
            <td></td>
        </tr>
    </tbody>
</table>



### Define effects

At this point, you’ll start to see some effects that you would like to have as a result of the user
interacting with your program. Add these effects to the effects column. In the login example,
whenever the user clicks on the login button, the program is supposed to attempt logging in.
Whenever the user clicks on the forgot password button, a restore password screen is supposed to
appear.

<table>
    <thead>
        <tr>
            <th align="center">Events</th>
            <th align="center">Model</th>
            <th align="center">Effects</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td align="center">InternetStateChanged</td>
            <td rowspan=5></td>
            <td align="center"><b><i>AttemptLogin</i></b></td>
        </tr>
        <tr>
            <td align="center">EmailInputChanged</td>
            <td align="center"><b><i>ShowRestorePasswordScreen</i></b></td>
        </tr>
        <tr>
            <td align="center">PasswordInputChanged</td>
            <td></td>
        </tr>
        <tr>
            <td align="center">LoginButtonClicked</td>
            <td></td>
        </tr>
        <tr>
            <td align="center">ForgotPasswordClicked</td>
            <td></td>
        </tr>
    </tbody>
</table>

### Define your model

By this point, you have defined a set of events and effects. However, in most cases, events cannot
directly result in effects and that is why your Update function takes two arguments; the current
model and the event. Your model should represent the state of your feature. It should contain all
the information necessary to help you make decisions about whether to dispatch effects and what the
next state of your system should be.

In our example, we see that in order to log in:

* It is necessary to be online.
* It is necessary to have an email and a password.
* It is necessary to keep track of whether there’s a login attempt in progress to avoid unnecessary
  further attempts until the current attempt has completed. Furthermore, this piece of information
  can be used to inform the user about the login attempt in progress.
* Finally, it is desirable that the login button is only enabled when there is a valid email and
  password, so we need to keep track of that in our model as well

<table>
    <thead>
        <tr>
            <th align="center">Events</th>
            <th align="center">Model</th>
            <th align="center">Effects</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td align="center">InternetStateChanged</td>
            <td rowspan=5 align="center" valign="top"><b><i>online?
                        <br>email
                        <br>password
                        <br>loggingIn?
                        <br>canLogin?</i></b>
                        </td>
            <td align="center">AttemptLogin</td>
        </tr>
        <tr>
            <td align="center">EmailInputChanged</td>
            <td align="center">ShowRestorePasswordScreen</td>
        </tr>
        <tr>
            <td align="center">PasswordInputChanged</td>
            <td align="center"></td>
        </tr>
        <tr>
            <td align="center">LoginButtonClicked</td>
            <td align="center"></td>
        </tr>
        <tr>
            <td align="center">ForgotPasswordClicked</td>
            <td align="center"></td>
        </tr>
    </tbody>
</table>

### Define effects feedback events

Once you have the model structure that will allow you to make decisions about dispatching effects in
place, you can move on to defining the third kind of events: effect feedback (note that not all
effects must necessarily generate feedback events). In our example, we see that of the two effects
we have defined, only the AttemptLogin effect could generate feedback events: LoginSuccessful and
LoginFailed.
<table>
    <thead>
        <tr>
            <th align="center">Events</th>
            <th align="center">Model</th>
            <th align="center">Effects</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td align="center">InternetStateChanged</td>
            <td rowspan=7 align="center" valign="top">online?
                        <br>email
                        <br>password
                        <br>loggingIn?
                        <br>canLogin?
                        </td>
            <td align="center">AttemptLogin</td>
        </tr>
        <tr>
            <td align="center">EmailInputChanged</td>
            <td align="center">ShowRestorePasswordScreen</td>
        </tr>
        <tr>
            <td align="center">PasswordInputChanged</td>
            <td align="center"></td>
        </tr>
        <tr>
            <td align="center">LoginButtonClicked</td>
            <td align="center"></td>
        </tr>
        <tr>
            <td align="center">ForgotPasswordClicked</td>
            <td align="center"></td>
        </tr>
        <tr>
            <td align="center"><b><i>LoginSuccessful</i></b></td>
            <td align="center"></td>
        </tr>
        <tr>
            <td align="center"><b><i>LoginFailed</i></b></td>
            <td align="center"></td>
        </tr>
    </tbody>
</table>

Each time you add events, you should go back to the Effect definition step until you’ve defined all
events and effects for your domain and expanded your model to contain the required information for
processing all events and dispatching all effects. In our example, we see that we can add a couple
of more effects to respond to the LoginSuccessful and LoginFailed events.
<table>
    <thead>
        <tr>
            <th align="center">Events</th>
            <th align="center">Model</th>
            <th align="center">Effects</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td align="center">InternetStateChanged</td>
            <td rowspan=7 align="center" valign="top">online?
                        <br>email
                        <br>password
                        <br>loggingIn?
                        <br>canLogin?
                        </td>
            <td align="center">AttemptLogin</td>
        </tr>
        <tr>
            <td align="center">EmailInputChanged</td>
            <td align="center">ShowRestorePasswordScreen</td>
        </tr>
        <tr>
            <td align="center">PasswordInputChanged</td>
            <td align="center"><b><i>NavigateToHome</i></b></td>
        </tr>
        <tr>
            <td align="center">LoginButtonClicked</td>
            <td align="center"><b><i>ShowErrorToast</i></b></td>
        </tr>
        <tr>
            <td align="center">ForgotPasswordClicked</td>
            <td align="center"></td>
        </tr>
        <tr>
            <td align="center">LoginSuccessful</td>
            <td align="center"></td>
        </tr>
        <tr>
            <td align="center">LoginFailed</td>
            <td align="center"></td>
        </tr>
    </tbody>
</table>

## Step 2: Describe it

Now that you have established all the necessary details that are required to write your behavior
specification, you can start building your Update function.

Building an Update function comprises the following steps:

1. Think of a scenario that your tests need to cover, eg.

> **given** that the user is offline
>
> **when** the login button is clicked
>
> **then** show an error message

2. Create/modify your Event, Effect, and Model types

```java

@DataEnum
interface Event_dataenum {
    // ...
    dataenum_case LoginButtonClicked();
    // ...
}

@DataEnum
interface Effect_dataenum {
    // ...
    dataenum_case ShowErrorToast(String message);
    // ...
}

@AutoValue
static abstract class Model {
    // ...
    public abstract boolean connected();
    // ...
}
```

3. Write tests for the scenario

```java
spec.given(Model.builder()
        .connected(false)
        .build())
        .when(Event.loginButtonClicked())
        .then(assertThatNext(hasEffect(
        Effect.showErrorToast("must be online to log in")
        )));
```

4. Change the code in your Update function to make the new tests pass

```java
// ...

if(event.isLoginButtonClicked()&&!model.connected()){
        return dispatch(effects(Effect.showErrorToast("must be online to log in")));
        }

// ...
```

5. Repeat steps 1 through 4 until you’ve exhausted all scenarios

6. Think of some unlikely scenarios and write some more tests!

This step is really about solidifying what you’ve captured in the first step. It’ll also help you
find out if you’ve missed certain behaviors or if some behaviors conflict with others. It also helps
you validate the completeness of your model. Once you’re done writing your tests and Update
function, you would have described in detail how your program will behave and answered all unknown
questions early on. This step usually doesn’t take more than a day since it is relatively easy to
write Update functions thanks to their pure nature.

## Step 3: Plan it

Now that you’ve defined your Events, Effects, Model and Update, you can use this information to
derive tasks that describe what work needs to be done for your project. You’ll notice that most of
the things that you need to build will produce one or more of the Events that you’ve defined. We
recommend that you cross-check your list of events and determine what components you need to build
to produce these events. The idea is to define tasks for building the blocks that interact with your
Update function.

### Event sources

Start with your EventSources which provide your external events. In our example, we had only one
event that is categorized as external: InternetStateChanged. So we know that we’ll have to add a
task for building that EventSource.

### Effects and their feedback events

Once you’ve created tasks for the external event sources, move on to Effects. We’ve found that it is
best to create an effect handler per effect. Create a task for each of your effects and associate
their feedback events with that task.

### User interaction events

This is where you start breaking up your UI into tasks. If your UI is simple, you could potentially
have only one task for building it. However, for complex UIs we recommend dividing them into smaller
pieces and associating events with each of these pieces.

Now that you’ve defined your different tasks, you can group them together to create user stories
that you can add to your project tracking tool. This process works whether you use Scrum, Kanban or
any other agile (or even waterfall) process framework since it produces a list of the work that
needs to happen, but it lets you decide how to manage the building process the way you like.
Furthermore, you can use the tasks that you’ve made to identify dependencies you may have on other
work that needs to be dealt with before you start.

## Step 4: Build it

Time to write some more code! Check out
the [getting started guide](./getting-started/Creating-a-loop.md) for examples on how to build
effect handlers and connect your UI to Mobius.
