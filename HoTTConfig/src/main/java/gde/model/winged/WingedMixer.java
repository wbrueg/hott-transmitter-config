package gde.model.winged;

import gde.model.Switch;
import gde.model.enums.SwitchFunction;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlIDREF;

public class WingedMixer {
	private static int NEXT_NUMBER = 0;

	private SwitchFunction function;
	private String number;
	private Switch sw;
	private int value;

	public WingedMixer(final SwitchFunction function) {
		this.function = function;
		number = Integer.toString(NEXT_NUMBER++);
	}

	public SwitchFunction getFunction() {
		return function;
	}

	@XmlID
	@XmlAttribute
	public String getNumber() {
		return number;
	}

	@XmlIDREF
	public Switch getSwitch() {
		return sw;
	}

	public int getValue() {
		return value;
	}

	public void setFunction(final SwitchFunction function) {
		this.function = function;
	}

	public void setNumber(final String number) {
		this.number = number;
	}

	public void setSwitch(final Switch sw) {
		this.sw = sw;
	}

	public void setValue(final int value) {
		this.value = value;
	}
}
