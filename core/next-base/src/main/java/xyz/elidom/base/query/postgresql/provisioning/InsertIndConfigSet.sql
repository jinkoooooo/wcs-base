insert into
	ind_config_set
select
	uuid_generate_v4() as id,
	stage_cd,
	ind_type,
	com_cd,
	job_type,
	equip_type,
	equip_cd,
	conf_set_cd,
	conf_set_nm,
	default_flag,
	remark,
	:targetDomainId as domain_id,
	creator_id,
	updater_id,
	created_at,
	updated_at
from
	ind_config_set
where
	domain_id = :sourceDomainId
	and default_flag = true
