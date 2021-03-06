package controllers

import javax.inject.{Inject, Singleton}

import jp.t2v.lab.play2.auth.AuthenticationElement
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services.{ItemService, UserService}

@Singleton
class UsersController @Inject()(val userService: UserService,
                                val itemService: ItemService,
                                components: ControllerComponents)
  extends AbstractController(components)
    with I18nSupport
    with AuthConfigSupport
    with AuthenticationElement {

  def show(id: Long): Action[AnyContent] = StackAction { implicit request =>
    (for {
      targetUser <- userService.findById(id)
      items <- itemService.getItemsByUserId(targetUser.get.id.get)
    } yield {
      Ok(views.html.users.show(loggedIn, targetUser.get, items))
    })
      .getOrElse(InternalServerError(Messages("InternalError")))
  }

}