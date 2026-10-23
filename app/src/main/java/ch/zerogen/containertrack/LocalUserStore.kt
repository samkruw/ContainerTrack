package ch.zerogen.containertrack

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Device-local profiles. No passwords, tokens, or user data leave this installation. */
class LocalUserStore(context:Context,preferenceName:String="local-users",private val clock:()->Long=System::currentTimeMillis) {
    private data class Record(val user:LocalUser,val pin:String,val failures:Int=0,val lockedUntil:Long=0)
    private val prefs=context.getSharedPreferences(preferenceName,Context.MODE_PRIVATE)
    private var records:List<Record> = decode(prefs.getString("profiles",null))
    private val mutableUsers=MutableStateFlow(records.map{it.user})
    val users=mutableUsers.asStateFlow()
    private val mutableCurrent=MutableStateFlow<LocalUser?>(null)
    val currentUser=mutableCurrent.asStateFlow()
    @Synchronized fun requireUser():LocalUser {
        val current=mutableCurrent.value ?: error("Bitte lokal anmelden.")
        return records.firstOrNull{it.user.id==current.id && it.user.active}?.user ?: error("Benutzer ist nicht aktiv.")
    }
    @Synchronized fun requireAccess(owner:String?) {check(mayAccessLocalJob(requireUser(),owner)){"Kein Zugriff auf diesen Auftrag."}}
    private fun admin() {check(requireUser().role==LocalRole.ADMIN){"Nur Administratoren dürfen Benutzer verwalten."}}
    private fun validatedName(value:String,excluding:String?=null):String {
        val clean=value.trim();require(clean.isNotEmpty() && clean.length<=60){"Name muss 1–60 Zeichen enthalten."}
        require(records.none{it.user.id!=excluding && it.user.name.equals(clean,ignoreCase=true)}){"Dieser Benutzername ist bereits vergeben."}
        return clean
    }
    private fun validPin(pin:String) {require(Regex("[0-9]{6,12}").matches(pin)){"PIN muss 6–12 Ziffern enthalten."}}
    @Synchronized fun bootstrap(name:String,pin:String) {
        check(records.isEmpty()){"Ein Administrator ist bereits eingerichtet."};validPin(pin)
        val user=LocalUser("local:${UUID.randomUUID()}",validatedName(name),LocalRole.ADMIN)
        save(listOf(Record(user,LocalPin.hash(pin))));mutableCurrent.value=user
    }
    @Synchronized fun login(id:String,pin:String) {
        val record=records.firstOrNull{it.user.id==id && it.user.active} ?: error("Benutzer ist nicht aktiv.")
        check(clock()>=record.lockedUntil){"Zu viele Versuche. Bitte eine Minute warten."}
        if(!LocalPin.verify(pin,record.pin)) {
            val count=record.failures+1
            save(records.map{if(it.user.id==id)it.copy(failures=count,lockedUntil=if(count>=5)clock()+60_000 else 0) else it})
            error("PIN ist nicht korrekt.")
        }
        save(records.map{if(it.user.id==id)it.copy(failures=0,lockedUntil=0) else it})
        mutableCurrent.value=record.user
    }
    @Synchronized fun logout() {mutableCurrent.value=null}
    @Synchronized fun add(name:String,role:LocalRole,pin:String) {
        admin();validPin(pin)
        val user=LocalUser("local:${UUID.randomUUID()}",validatedName(name),role)
        save(records+Record(user,LocalPin.hash(pin)))
    }
    @Synchronized fun update(id:String,name:String,role:LocalRole,active:Boolean,pin:String?=null) {
        admin();val existing=records.firstOrNull{it.user.id==id} ?: error("Benutzer fehlt.")
        val clean=validatedName(name,id)
        if(pin!=null)validPin(pin)
        val updated=existing.copy(user=existing.user.copy(name=clean,role=role,active=active),pin=pin?.let(LocalPin::hash) ?: existing.pin,
            failures=if(pin!=null)0 else existing.failures,lockedUntil=if(pin!=null)0 else existing.lockedUntil)
        val next=records.map{if(it.user.id==id)updated else it}
        check(next.any{it.user.active && it.user.role==LocalRole.ADMIN}){"Mindestens ein aktiver Administrator muss erhalten bleiben."}
        save(next)
    }
    @Synchronized fun changeOwnPin(oldPin:String,newPin:String) {
        val user=requireUser();login(user.id,oldPin);validPin(newPin)
        save(records.map{if(it.user.id==user.id)it.copy(pin=LocalPin.hash(newPin),failures=0,lockedUntil=0) else it})
    }
    private fun save(next:List<Record>) {
        val array=JSONArray()
        next.forEach{r->array.put(JSONObject().put("id",r.user.id).put("name",r.user.name).put("role",r.user.role.name).put("active",r.user.active)
            .put("pin",r.pin).put("failures",r.failures).put("lockedUntil",r.lockedUntil))}
        check(prefs.edit().putString("profiles",array.toString()).commit()){"Benutzer konnten nicht gespeichert werden."}
        records=next;mutableUsers.value=next.map{it.user}
        val current=mutableCurrent.value
        if(current!=null)mutableCurrent.value=next.firstOrNull{it.user.id==current.id && it.user.active}?.user
    }
    private fun decode(text:String?):List<Record> {
        if(text==null)return emptyList()
        val array=JSONArray(text)
        return (0 until array.length()).map{index->val r=array.getJSONObject(index)
            Record(LocalUser(r.getString("id"),r.getString("name"),LocalRole.valueOf(r.getString("role")),r.getBoolean("active")),r.getString("pin"),r.optInt("failures"),r.optLong("lockedUntil"))}
    }
}
