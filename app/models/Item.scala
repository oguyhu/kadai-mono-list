package models

import java.time.ZonedDateTime

import scalikejdbc._
import jsr310._
import skinny.orm.feature.CRUDFeatureWithId
import skinny.orm.feature.associations.HasManyAssociation
import skinny.orm.{Alias, SkinnyCRUDMapper}

case class Item(id: Option[Long],
                code: String,
                name: String,
                url: String,
                imageUrl: String,
                price: Int,
                createAt: ZonedDateTime,
                updateAt: ZonedDateTime,
                users: Seq[User] = Seq.empty,
                wantUsers: Seq[User] = Seq.empty,
                haveUsers: Seq[User] = Seq.empty)

object Item extends SkinnyCRUDMapper[Item] {

  lazy val allAssociations: CRUDFeatureWithId[Long, Item] = joins(allUsersRef, wantUsersRef, haveUsersRef)

  val allUsersRef: HasManyAssociation[Item] = hasManyThrough[User](
    through = ItemUser,
    many = User,
    merge = (item, users) => item.copy(users = users)
  )

  val wantUsersRef: HasManyAssociation[Item] = hasManyThrough[ItemUser, User](
    through = ItemUser -> ItemUser.defaultAlias,
    throughOn = (item: Alias[Item], itemUser: Alias[ItemUser]) => sqls.eq(item.id, itemUser.itemId),
    many = User -> User.createAlias("i_in_u_want"),
    on = (itemUser: Alias[ItemUser], user: Alias[User]) =>
      sqls.eq(itemUser.userId, user.id).and.eq(itemUser.`type`, WantHaveType.Want.toString),
    merge = (item, wantUsers) => item.copy(wantUsers = wantUsers)
  )

  //throughは中間テーブル
  //throughOnは中間テーブルとの結合条件
  //manyは中間テーブルを通して結合するテーブル
  //onはそのテーブルと中間テーブルとの結合条件
  val haveUsersRef: HasManyAssociation[Item] = hasManyThrough[ItemUser, User](
    through = ItemUser -> ItemUser.defaultAlias,
    throughOn = (item: Alias[Item], itemUser: Alias[ItemUser]) => sqls.eq(item.id, itemUser.itemId),
    many = User -> User.createAlias("i_in_u_have"),
    on = (itemUser: Alias[ItemUser], user: Alias[User]) =>
      sqls.eq(itemUser.userId, user.id).and.eq(itemUser.`type`, WantHaveType.Have.toString),
    merge = (item, haveUsers) => item.copy(haveUsers = haveUsers)
  )

  override def tableName: String = "items"

  override def defaultAlias: Alias[Item] = createAlias("i")

  override def extract(rs: WrappedResultSet, n: ResultName[Item]): Item =
    autoConstruct(rs, n, "users", "wantUsers", "haveUsers")

  private def toNamedValues(record: Item): Seq[(Symbol, Any)] = Seq(
    'code     -> record.code,
    'name     -> record.name,
    'url      -> record.url,
    'imageUrl -> record.imageUrl,
    'price    -> record.price,
    'createAt -> record.createAt,
    'updateAt -> record.updateAt
  )

  def create(item: Item)(implicit session: DBSession = AutoSession): Long =
    createWithAttributes(toNamedValues(item): _*)

  def update(item: Item)(implicit session: DBSession = AutoSession): Int =
    updateById(item.id.get).withAttributes(toNamedValues(item): _*)


}