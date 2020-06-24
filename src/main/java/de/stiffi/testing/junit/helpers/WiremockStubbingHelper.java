package de.stiffi.testing.junit.helpers;

import javax.validation.constraints.NotNull;
import java.io.IOException;

public class WiremockStubbingHelper extends WiremockHelper {


	public WiremockStubbingHelper(String wiremockUrl) {
		super(wiremockUrl);
	}

	public VMDSEntryBuilder mockVMDSBuilder(@NotNull String vin, @NotNull String csmType) {
		return new VMDSEntryBuilder().withVin(vin).withCsmType(csmType);
	}

	public class VMDSEntryBuilder {

		private static final String NOT_SET = "NOT_MOCKED";

		private String vin = NOT_SET;
		private String csmSnr = NOT_SET;
		private String csmType = NOT_SET;
		private String csmSimMsisdn = NOT_SET;
		private String designatedProvisioningEnvironment = NOT_SET;
		private String fleet = NOT_SET;

		protected VMDSEntryBuilder() {
		}

		public VMDSEntryBuilder withVin(String vin) {
			this.vin = vin;
			return this;
		}

		public VMDSEntryBuilder withCsmSnr(String csmSnr) {
			this.csmSnr = csmSnr;
			return this;
		}

		public VMDSEntryBuilder withCsmType(String csmType) {
			this.csmType = csmType;
			return this;
		}


		public VMDSEntryBuilder withCsmSimMsIsdn(String csmSimMsisdn) {
			this.csmSimMsisdn = csmSimMsisdn;
			return this;
		}

		public VMDSEntryBuilder withDesignatedProvisioningEnvironment(String designatedProvisioningEnvironment) {
			this.designatedProvisioningEnvironment = designatedProvisioningEnvironment;
			return this;
		}

		public VMDSEntryBuilder withFleet(String fleet) {
			this.fleet = fleet;
			return this;
		}

		public void build() throws IOException {
			String response = TemplateHelperFactory.getDefault()
					.withVin(vin).withCsmSnr(csmSnr)
					.with("{{csmType}}", csmType)
					.with("{{csmSimMsisdn}}", csmSimMsisdn)
					.with("{{fleet}}", fleet)
					.with("{{designatedProvisioningEnvironment}}", designatedProvisioningEnvironment)
					.readTemplateFile("Wiremock-VMDS-Template.json");
			String urlPattern = "\\/vmds\\/vehicles-v3\\\\?filter=vin%3D%3D" + vin + ".*";
			createMapping("GET", urlPattern, response, "application/json", 200);
		}
	}
}
