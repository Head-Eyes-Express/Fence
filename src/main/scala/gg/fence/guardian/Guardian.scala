package gg.fence.guardian

import akka.actor.typed.Behavior
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}

object Guardian {

  sealed trait Msg

  case object Hello extends Msg
  case object World extends Msg

  sealed trait Evt

  case object Replied extends Evt

  case class State(messageCounter: Int, replyCounter: Int)

  private val initState = State(messageCounter = 0, replyCounter = 0)

  private def commandHandler(state: State, command: Msg): Effect[Evt, State] = command match {
    case Hello => Effect.persist(Replied).thenRun { _ =>
      println(s"Current state: ${state}")
      println("Hello")
    }
    case World => Effect.persist(Replied).thenRun { _ =>
      println(s"Current state: ${state}")
      println("Hello")
    }
  }

  private def evtHandler(currentState: State, evt: Evt): State =
    evt match {
      case Replied => currentState.copy(currentState.replyCounter + 1, currentState.messageCounter + 1)
    }

  def apply(): Behavior[Msg] =
    EventSourcedBehavior[Msg, Evt, State](
      persistenceId = PersistenceId.ofUniqueId("guardian"),
      emptyState = initState,
      commandHandler = commandHandler,
      eventHandler = evtHandler,
    )

}
