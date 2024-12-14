package Project.Common;

public class PickPayload extends Payload {
    private String pick;

    public PickPayload(String pick){
        setPayloadType(PayloadType.PICK);
    }

    public String getPick(){
        return pick;
    }

    public void setPick(String pick){
        this.pick = pick;
    }
    
    
}
