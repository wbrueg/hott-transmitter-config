package gde.model;

import java.util.List;

abstract public class BaseModel {
	private Vendor vendor;
	private TransmitterType transmitterType;
	private int appVersion;
	private int memoryVersion;
	private ModelType modelType;
	private String modelName;
	private String info;
	private boolean bound;
	private StickMode stickMode;
	private HFModule module;
	private DSCOutputType dscOutput;
	private ExtPPMType extPpmType;
	private ThrottleCutOf throttleCutOf;
	private Switch powerOnWarning;
	private Switch autoTrimSwitch;
	private boolean autoTimerReset;
	private List<Servo> servos;
	private List<Stick> sticks;
	private List<Control> controls;
	private List<Switch> switches;
	private List<Mixer> mixers;
	private List<Clock> clocks;
	private List<Phase> phases;
	private int modelNumber;
	private long transmitterId;
	private long receiverId;

	public BaseModel(final ModelType modelType) {
		setModelType(modelType);
	}

	public int getAppVersion() {
		return appVersion;
	}

	public Switch getAutoTrimSwitch() {
		return autoTrimSwitch;
	}

	public List<Clock> getClocks() {
		return clocks;
	}

	public List<Control> getControls() {
		return controls;
	}

	public DSCOutputType getDscOutput() {
		return dscOutput;
	}

	public ExtPPMType getExtPpmType() {
		return extPpmType;
	}

	public String getInfo() {
		return info;
	}

	public int getMemoryVersion() {
		return memoryVersion;
	}

	public List<Mixer> getMixers() {
		return mixers;
	}

	public String getModelName() {
		return modelName;
	}

	public int getModelNumber() {
		return modelNumber;
	}

	public ModelType getModelType() {
		return modelType;
	}

	public HFModule getModule() {
		return module;
	}

	public List<Phase> getPhases() {
		return phases;
	}

	public Switch getPowerOnWarning() {
		return powerOnWarning;
	}

	public long getReceiverId() {
		return receiverId;
	}

	public List<Servo> getServos() {
		return servos;
	}

	public StickMode getStickMode() {
		return stickMode;
	}

	public List<Stick> getSticks() {
		return sticks;
	}

	public List<Switch> getSwitches() {
		return switches;
	}

	public ThrottleCutOf getThrottleCutOf() {
		return throttleCutOf;
	}

	public long getTransmitterId() {
		return transmitterId;
	}

	public TransmitterType getTransmitterType() {
		return transmitterType;
	}

	public Vendor getVendor() {
		return vendor;
	}

	public boolean isAutoTimerReset() {
		return autoTimerReset;
	}

	public boolean isBound() {
		return bound;
	}

	public void setAppVersion(final int appVersion) {
		this.appVersion = appVersion;
	}

	public void setAutoTimerReset(final boolean autoTimerReset) {
		this.autoTimerReset = autoTimerReset;
	}

	public void setAutoTrimSwitch(final Switch autoTrimSwitch) {
		this.autoTrimSwitch = autoTrimSwitch;
	}

	public void setBound(final boolean bound) {
		this.bound = bound;
	}

	public void setClocks(final List<Clock> clocks) {
		this.clocks = clocks;
	}

	public void setControls(final List<Control> controls) {
		this.controls = controls;
	}

	public void setDscOutput(final DSCOutputType dscOutput) {
		this.dscOutput = dscOutput;
	}

	public void setExtPpmType(final ExtPPMType extPpmType) {
		this.extPpmType = extPpmType;
	}

	public void setInfo(final String info) {
		this.info = info;
	}

	public void setMemoryVersion(final int memoryVersion) {
		this.memoryVersion = memoryVersion;
	}

	public void setMixers(final List<Mixer> mixers) {
		this.mixers = mixers;
	}

	public void setModelName(final String modelName) {
		this.modelName = modelName;
	}

	public void setModelNumber(final int modelNumber) {
		this.modelNumber = modelNumber;
	}

	public void setModelType(final ModelType modelType) {
		this.modelType = modelType;
	}

	public void setModule(final HFModule module) {
		this.module = module;
	}

	public void setPhases(final List<Phase> phases) {
		this.phases = phases;
	}

	public void setPowerOnWarning(final Switch powerOnWarning) {
		this.powerOnWarning = powerOnWarning;
	}

	public void setReceiverId(final long receiverId) {
		this.receiverId = receiverId;
	}

	public void setServos(final List<Servo> servos) {
		this.servos = servos;
	}

	public void setStickMode(final StickMode stickMode) {
		this.stickMode = stickMode;
	}

	public void setSticks(final List<Stick> sticks) {
		this.sticks = sticks;
	}

	public void setSwitches(final List<Switch> switches) {
		this.switches = switches;
	}

	public void setThrottleCutOf(final ThrottleCutOf throttleCutOf) {
		this.throttleCutOf = throttleCutOf;
	}

	public void setTransmitterId(final long transmitterId) {
		this.transmitterId = transmitterId;
	}

	public void setTransmitterType(final TransmitterType transmitterType) {
		this.transmitterType = transmitterType;
	}

	public void setVendor(final Vendor vendor) {
		this.vendor = vendor;
	}
}
