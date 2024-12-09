package Project.Common;

//679 12-8-2024

public class PointsPayload extends Payload {
private int changedPoints;
private int currentPoints;

public PointsPayload(){
    setPayloadType(PayloadType.POINTS);
}

public int getChangedPoints(){
    return changedPoints;
}

public void setChangedPoints(int changedPoints){
    this.changedPoints = changedPoints;
    
    }

    public int getCurrentPoints(){
        return currentPoints;
    }

public void setCurrentPoints(int currentPoints){
    this.currentPoints = currentPoints;
}
}

