package fi.vm.sade.ldap

import org.json4s.DefaultFormats

class LdapClient(config: LdapConfig) {
  import org.json4s.jackson._
  import scala.collection.JavaConversions._
  import com.unboundid.ldap.sdk.{Filter, LDAPConnection, SearchRequest, SearchScope}

  implicit val formats = DefaultFormats

  def findUser(userid: String) = {
    val connection: LDAPConnection = new LDAPConnection()
    connection.connect(config.host, 389)
    try {
      connection.bind(config.userDn, config.password)
      val filter = Filter.createEqualityFilter("uid", userid);
      val searchRequest = new SearchRequest("ou=People,dc=opintopolku,dc=fi", SearchScope.SUB, filter, "description", "uid");
      connection.search(searchRequest).getSearchEntries.toList.headOption.map { result =>
        val roles = Option(result.getAttribute("description").getValue).toList.flatMap { crazyString =>
          parseJson(crazyString).extract[List[String]]
        }
        LdapUser(roles)
      }
    } finally {
      connection.close
    }
  }
}