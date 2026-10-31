package jp.arils.belowzero;
import java.nio.charset.StandardCharsets;
public final class NegotiationCheck {
 static int checks;
 static void require(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
 static byte[] channels(String value){return value.getBytes(StandardCharsets.UTF_8);}
 public static void main(String[] args){
  Object server=new Object(),other=new Object(),world=new Object(),nether=new Object();
  BridgeNegotiation p=new BridgeNegotiation();p.connect(server);
  require(!p.request(server,world,0),"No unsolicited handshake on vanilla login");
  require(!p.accepts(server,world),"Unsolicited server hello must not enable the mod");
  require(!p.channels(server,channels("other:plugin\0arils:belowzero_extra\0"),true),"Unrelated or prefix-only channels are not capabilities");
  require(!p.request(server,world,0),"Normal Bukkit plugin registration stays vanilla");
  require(p.channels(server,channels("other:plugin\0arils:belowzero\0"),true),"Exact companion advertisement is detected");
  require(p.request(server,world,0),"Advertised companion can be requested");
  require(!p.request(server,world,1),"Repeated advertisements do not spam requests for the same world");
  require(p.accepts(server,world),"Current world accepts negotiated hello");
  require(!p.accepts(other,world)&&!p.accepts(server,nether),"Other connections and worlds are isolated");
  p.confirmed(server,world);
  require(!p.waiting()&&p.accepts(server,world),"Companion epoch refreshes remain compatible after confirmation");
  require(!p.expire(10_000_000_000L),"Confirmed companion does not time out");
  p.worldChanged(server);
  require(p.advertised()&&!p.accepts(server,world),"Respawn clears world state but keeps server capability");
  require(p.request(server,nether,20),"New dimension negotiates afresh");
  require(!p.expire(4_000_000_000L),"Pending handshake has a bounded grace period");
  require(p.expire(6_000_000_000L)&&!p.accepts(server,nether),"Missing companion response falls back and rejects late replies");
  p.worldChanged(server);require(p.request(server,world,30),"A later world may negotiate again");p.confirmed(server,world);
  require(p.channels(server,channels("arils:belowzero\0"),false)&&!p.advertised()&&!p.accepts(server,world),"UNREGISTER disables the companion");
  p.connect(other);
  require(!p.channels(server,channels("arils:belowzero\0"),true),"Old connection registration cannot affect a new server");
  require(!p.request(other,world,50),"Plugin-to-vanilla transition sends nothing");
  p.disconnect();require(!p.owns(other)&&!p.advertised(),"Disconnect erases server capability");
  System.out.println("Negotiation checks passed: "+checks);
 }
}
