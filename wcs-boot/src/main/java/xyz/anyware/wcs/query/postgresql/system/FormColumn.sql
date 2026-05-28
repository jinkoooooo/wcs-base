select
	b.name,
	b.term ,
	b.ref_type,
	b.ref_name ,
	b.ref_name,
	b.grid_editor as search_editor,
	coalesce(b.search_oper, 'eq') as search_oper,
	b.search_rank
from
	vue_menus a,
	menu_columns b
where
	a.name =:router
	and a.id = b.menu_id
	and (b.grid_editor is not null )
order by
	b.grid_rank