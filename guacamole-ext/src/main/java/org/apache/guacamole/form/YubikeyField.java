package org.apache.guacamole.form;

public class YubikeyField extends Field{
    
	/**
     * Creates a new YubikeyField with the given name.
     *
     * @param name
     *     The unique name to associate with this field.
     */
    public YubikeyField(String name) {
        super(name, Field.Type.YUBIKEY);
    }

}
