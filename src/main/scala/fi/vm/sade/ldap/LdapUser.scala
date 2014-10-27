package fi.vm.sade.ldap

case class LdapUser(roles: List[String]) {
  def hasRole(role: String) = {
    roles.contains(role)
  }
}
